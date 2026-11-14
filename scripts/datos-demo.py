"""Carga datos de ejemplo en una instancia local de Kardexa y recorre el flujo de ventas.

Crea (si no existen) categorías, productos `DEMO-*`, un proveedor, una empleada de prueba y una compra
que da stock. Luego registra ventas como ADMIN y como EMPLEADO, anula una y comprueba las reglas
principales: stock insuficiente (RN-01), anulación solo del ADMIN (RN-05) y que el EMPLEADO solo ve sus
ventas (RF-21). Además deja productos en stock crítico (RN-09) y comprueba que el inicio (`/dashboard`)
cuadra con el historial de ventas. Solo usa la biblioteca estándar de Python.

Uso (con el backend corriendo):

    KARDEXA_ADMIN_EMAIL=admin@bodega.pe KARDEXA_ADMIN_PASSWORD=... python scripts/datos-demo.py

Si faltan las variables, las pide por consola. `--api` cambia la URL base (por defecto
http://localhost:8080/api/v1). La contraseña de la empleada de prueba se toma de
`KARDEXA_DEMO_PASSWORD` o se genera y se muestra una vez al crearla.

Ventas del último mes (`--repartir`): la API solo registra ventas con la hora actual, así que el script
registra por la API unas 60 ventas (algunas anuladas) y después les cambia la fecha (`sale.sold_at` y
`voided_at`) a los 29 días anteriores con `psql` dentro del contenedor de PostgreSQL de `docker compose`
(`--contenedor`, por defecto kardexa-postgres; usuario y base de `DB_USER` y `DB_NAME`, por defecto kardexa).
Los movimientos del kárdex son inmutables (RN-03) y conservan la fecha real de registro. Sin `--repartir`
no se registran esas ventas y el gráfico del inicio solo tiene el día de hoy.

Cada ejecución registra ventas nuevas: úsalo solo en una base local de desarrollo.
"""

import argparse
import getpass
import json
import os
import random
import secrets
import subprocess
import sys
import urllib.error
import urllib.request
from datetime import datetime, time, timedelta, timezone
from decimal import Decimal

EMPLOYEE_EMAIL = "empleada.demo@kardexa.test"

CATEGORIES = ["Abarrotes", "Bebidas", "Limpieza"]

# código, nombre, categoría, unidad, precio de venta, stock mínimo, cantidad comprada, costo unitario
PRODUCTS = [
    ("DEMO-ARZ", "Arroz extra 5 kg", "Abarrotes", "UNIT", "24.90", "5", "30", "19.50"),
    ("DEMO-AZU", "Azúcar rubia 1 kg", "Abarrotes", "UNIT", "4.80", "10", "40", "3.60"),
    ("DEMO-ACE", "Aceite vegetal 1 L", "Abarrotes", "UNIT", "9.90", "6", "24", "7.40"),
    ("DEMO-LEN", "Lentejas a granel", "Abarrotes", "KG", "7.50", "3", "12.5", "5.20"),
    ("DEMO-GAS", "Agua sin gas 625 ml", "Bebidas", "UNIT", "1.50", "24", "96", "0.90"),
    ("DEMO-GAL", "Gaseosa 1.5 L", "Bebidas", "UNIT", "6.50", "12", "36", "4.70"),
    ("DEMO-DET", "Detergente 900 g", "Limpieza", "UNIT", "11.90", "4", "15", "8.80"),
    ("DEMO-LEJ", "Lejía 1 L", "Limpieza", "UNIT", "3.20", "6", "3", "2.10"),
]

SUPPLIER = {"ruc": "20601234567", "businessName": "Distribuidora Demo SAC", "phone": "014567890"}

# Hora de Lima (sin horario de verano): los días del inicio se cuentan en esta zona.
LIMA = timezone(timedelta(hours=-5), "America/Lima")

# Ventas del último mes: días anteriores a hoy, productos que se venden y cantidad base de cada uno.
HISTORY_DAYS = 29
BASKET = [("DEMO-ARZ", "1"), ("DEMO-AZU", "2"), ("DEMO-ACE", "1"), ("DEMO-LEN", "0.5"), ("DEMO-GAS", "3"),
          ("DEMO-GAL", "1")]
VOIDED_IN_HISTORY = 4

# Productos que quedan en stock crítico (stock <= mínimo, RN-09) y el stock con el que quedan.
CRITICAL = {"DEMO-LEJ": Decimal("0"), "DEMO-DET": Decimal("2")}


class Api:
    def __init__(self, base):
        self.base = base.rstrip("/")
        self.token = None

    def call(self, method, path, body=None, expect=None):
        data = None if body is None else json.dumps(body).encode()
        request = urllib.request.Request(self.base + path, data=data, method=method)
        request.add_header("Content-Type", "application/json")
        if self.token:
            request.add_header("Authorization", "Bearer " + self.token)
        try:
            with urllib.request.urlopen(request) as response:
                status, raw = response.status, response.read()
        except urllib.error.HTTPError as error:
            status, raw = error.code, error.read()
        except urllib.error.URLError as error:
            sys.exit(f"No se pudo conectar con {self.base}: {error.reason}. ¿Está corriendo el backend?")
        payload = json.loads(raw) if raw else None
        if expect is not None and status != expect:
            detail = payload.get("detail") or payload.get("title") if isinstance(payload, dict) else payload
            sys.exit(f"{method} {path} respondió {status} (se esperaba {expect}): {detail}")
        return status, payload

    def login(self, email, password):
        _, body = self.call("POST", "/auth/login", {"email": email, "password": password}, expect=200)
        self.token = body["accessToken"]
        return body["user"]


def ensure_catalog(api):
    _, existing = api.call("GET", "/categories", expect=200)
    items = existing if isinstance(existing, list) else existing.get("content", [])
    categories = {c["name"]: c["id"] for c in items}
    for name in CATEGORIES:
        if name not in categories:
            _, created = api.call("POST", "/categories", {"name": name}, expect=201)
            categories[name] = created["id"]

    products = {}
    for code, name, category, unit, price, min_stock, *_ in PRODUCTS:
        _, page = api.call("GET", f"/products?q={code}&includeInactive=true&size=5", expect=200)
        found = next((p for p in page["content"] if p["code"] == code), None)
        if found is None:
            _, found = api.call("POST", "/products", {
                "code": code, "name": name, "categoryId": categories[category], "unit": unit,
                "salePrice": price, "minStock": min_stock,
            }, expect=201)
            print(f"  producto creado: {code} {name}")
        products[code] = found
    return products


def ensure_supplier(api):
    _, suppliers = api.call("GET", f"/suppliers?q={SUPPLIER['ruc']}", expect=200)
    items = suppliers if isinstance(suppliers, list) else suppliers.get("content", [])
    found = next((s for s in items if s["ruc"] == SUPPLIER["ruc"]), None)
    if found is None:
        _, found = api.call("POST", "/suppliers", SUPPLIER, expect=201)
        print(f"  proveedor creado: {SUPPLIER['businessName']}")
    return found


def ensure_employee(api):
    _, users = api.call("GET", "/users?size=100", expect=200)
    if any(u["email"] == EMPLOYEE_EMAIL for u in users["content"]):
        password = os.environ.get("KARDEXA_DEMO_PASSWORD")
        if not password:
            print("  la empleada de prueba ya existe; define KARDEXA_DEMO_PASSWORD para usarla")
        return password
    password = os.environ.get("KARDEXA_DEMO_PASSWORD") or secrets.token_urlsafe(12)
    api.call("POST", "/users", {"fullName": "Empleada Demo", "email": EMPLOYEE_EMAIL, "password": password,
                                "role": "EMPLOYEE"}, expect=201)
    print(f"  empleada de prueba creada: {EMPLOYEE_EMAIL} / {password}")
    return password


def stock(api, product_id):
    _, product = api.call("GET", f"/products/{product_id}", expect=200)
    return product["stock"]


def sell(api, products, lines, expect=201):
    items = [{"productId": products[code]["id"], "quantity": qty} for code, qty in lines]
    return api.call("POST", "/sales", {"items": items}, expect=expect)[1]


def today():
    return datetime.now(LIMA).date()


def plan_history(rng):
    """Ventas de los días anteriores a hoy, en orden cronológico: (fecha y hora en Lima, líneas)."""
    plan = []
    for offset in range(HISTORY_DAYS, 0, -1):
        day = today() - timedelta(days=offset)
        count = rng.randint(1, 3) + (1 if day.weekday() >= 5 else 0)
        for minute in sorted(rng.sample(range(8 * 60, 20 * 60), count)):
            lines = [(code, str(Decimal(qty) * rng.randint(1, 3)))
                     for code, qty in rng.sample(BASKET, rng.randint(1, 3))]
            plan.append((datetime.combine(day, time(minute // 60, minute % 60), LIMA), lines))
    return plan


def run_sql(args, sql):
    command = ["docker", "exec", "-i", args.contenedor, "psql", "-U", os.environ.get("DB_USER", "kardexa"),
               "-d", os.environ.get("DB_NAME", "kardexa"), "-v", "ON_ERROR_STOP=1", "-q"]
    try:
        result = subprocess.run(command, input=sql, capture_output=True, text=True, encoding="utf-8")
    except FileNotFoundError:
        sys.exit("No se encontró docker: --repartir necesita el contenedor de PostgreSQL.")
    if result.returncode != 0:
        sys.exit(f"psql falló en {args.contenedor}: {result.stderr.strip()}")


def register_history(args, admin, employee, products, supplier):
    """Registra por la API las ventas del último mes y luego les pone su fecha con psql."""
    rng = random.Random(146)
    plan = plan_history(rng)
    needed = {}
    for _, lines in plan:
        for code, qty in lines:
            needed[code] = needed.get(code, Decimal(0)) + Decimal(qty)
    costs = {code: cost for code, *_, cost in PRODUCTS}
    admin.call("POST", "/purchases", {
        "supplierId": supplier["id"], "supplierDocument": "F002-DEMO-MES",
        "purchaseDate": (today() - timedelta(days=HISTORY_DAYS + 1)).isoformat(),
        "items": [{"productId": products[code]["id"], "quantity": str(qty), "unitCost": costs[code]}
                  for code, qty in needed.items()],
    }, expect=201)

    dated = []
    for index, (moment, lines) in enumerate(plan):
        seller = employee if employee and index % 3 == 2 else admin
        dated.append((sell(seller, products, lines), moment))
    voided = set(rng.sample(range(len(dated)), VOIDED_IN_HISTORY))
    for index in sorted(voided):
        admin.call("POST", f"/sales/{dated[index][0]['id']}/void", {"reason": "Error al registrar la venta"},
                   expect=200)

    sql = ["BEGIN;"]
    for index, (sale, moment) in enumerate(dated):
        voided_at = f", voided_at = '{(moment + timedelta(hours=2)).isoformat()}'" if index in voided else ""
        sql.append(f"UPDATE sale SET sold_at = '{moment.isoformat()}'{voided_at} WHERE id = '{sale['id']}';")
    sql.append("COMMIT;")
    run_sql(args, "\n".join(sql) + "\n")
    print(f"  {len(dated)} ventas en los {HISTORY_DAYS} días anteriores, {len(voided)} anuladas")


def leave_critical(admin, products):
    """Vende lo necesario para que los productos de CRITICAL queden con stock <= mínimo."""
    lines = []
    for code, target in CRITICAL.items():
        current = Decimal(str(stock(admin, products[code]["id"])))
        if current > target:
            lines.append((code, str(current - target)))
    if lines:
        sale = sell(admin, products, lines)
        print(f"  {sale['number']}  S/ {sale['total']:>8}  deja en stock crítico: {', '.join(c for c, _ in lines)}")


def registered_sales(api, start, end):
    """Total y cantidad de las ventas registradas (no anuladas) del rango, recorriendo el historial."""
    total, count, page = Decimal(0), 0, 0
    while True:
        _, body = api.call("GET", f"/sales?from={start}&to={end}&size=100&page={page}", expect=200)
        for sale in body["content"]:
            if sale["status"] == "REGISTERED":
                total += Decimal(str(sale["total"]))
                count += 1
        page += 1
        if page >= body["totalPages"]:
            return total, count


def check(condition, message):
    print(("  OK   " if condition else "  FALLA ") + message)
    return condition


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--api", default="http://localhost:8080/api/v1", help="URL base de la API")
    parser.add_argument("--repartir", action="store_true",
                        help="registra ventas del último mes y les cambia la fecha con psql (solo desarrollo)")
    parser.add_argument("--contenedor", default="kardexa-postgres", help="contenedor de PostgreSQL para --repartir")
    args = parser.parse_args()

    email = os.environ.get("KARDEXA_ADMIN_EMAIL") or input("Correo del ADMIN: ")
    password = os.environ.get("KARDEXA_ADMIN_PASSWORD") or getpass.getpass("Contraseña del ADMIN: ")

    admin = Api(args.api)
    admin_user = admin.login(email, password)
    print(f"Sesión de ADMIN: {admin_user['fullName']}")

    print("\n1. Catálogo, proveedor y empleada")
    products = ensure_catalog(admin)
    supplier = ensure_supplier(admin)
    employee_password = ensure_employee(admin)
    employee = None
    if employee_password:
        employee = Api(args.api)
        employee.login(EMPLOYEE_EMAIL, employee_password)

    print("\n2. Compra que da stock (HU-12)")
    _, purchase = admin.call("POST", "/purchases", {
        "supplierId": supplier["id"], "supplierDocument": "F001-DEMO", "purchaseDate": today().isoformat(),
        "items": [{"productId": products[code]["id"], "quantity": qty, "unitCost": cost}
                  for code, _, _, _, _, _, qty, cost in PRODUCTS],
    }, expect=201)
    print(f"  compra registrada por S/ {purchase['total']}")

    print("\n3. Ventas del último mes y stock crítico (indicadores del inicio)")
    if args.repartir:
        register_history(args, admin, employee, products, supplier)
    else:
        print("  ventas del último mes omitidas: usa --repartir para llenar el gráfico del inicio")
    leave_critical(admin, products)

    print("\n4. Ventas del ADMIN (HU-14)")
    rice_before = stock(admin, products["DEMO-ARZ"]["id"])
    first = sell(admin, products, [("DEMO-ARZ", "2"), ("DEMO-ACE", "1"), ("DEMO-GAS", "6")])
    second = sell(admin, products, [("DEMO-LEN", "1.25"), ("DEMO-AZU", "3")])
    third = sell(admin, products, [("DEMO-GAL", "2"), ("DEMO-DET", "1")])
    for sale in (first, second, third):
        print(f"  {sale['number']}  S/ {sale['total']:>8}  {len(sale['items'])} productos")
    check(first["items"][0]["unitPrice"] == products["DEMO-ARZ"]["salePrice"], "el precio se copia del catálogo (RN-06)")
    numbers = [int(s["number"][2:]) for s in (first, second, third)]
    check(numbers == list(range(numbers[0], numbers[0] + 3)), "los números son correlativos (RN-07)")
    check(float(stock(admin, products["DEMO-ARZ"]["id"])) == float(rice_before) - 2, "la venta descuenta el stock")

    print("\n5. Reglas de negocio")
    lejia = stock(admin, products["DEMO-LEJ"]["id"])
    rejected = sell(admin, products, [("DEMO-ARZ", "1"), ("DEMO-LEJ", str(int(float(lejia)) + 5))], expect=422)
    check("Lejía" in (rejected.get("detail") or ""), f"stock insuficiente rechaza la venta y nombra el producto: {rejected.get('detail')}")
    _, kardex = admin.call("GET", f"/products/{products['DEMO-ARZ']['id']}/kardex", expect=200)
    check(any(m["type"] == "SALE" and m["sourceId"] == first["id"] for m in kardex["movements"]), "la venta deja su salida en el kárdex")

    print("\n6. Ventas de la EMPLEADA (RF-21)")
    if employee:
        own = sell(employee, products, [("DEMO-GAS", "2"), ("DEMO-AZU", "1")])
        print(f"  {own['number']}  S/ {own['total']:>8}  vendida por {own['seller']}")
        _, mine = employee.call("GET", "/sales?size=100", expect=200)
        check(all(s["sellerId"] == own["sellerId"] for s in mine["content"]), "la empleada solo ve sus ventas")
        status, _ = employee.call("GET", f"/sales/{first['id']}")
        check(status == 404, "la venta del ADMIN le responde 404")
        status, _ = employee.call("POST", f"/sales/{own['id']}/void", {"reason": "prueba"})
        check(status == 403, "la empleada no puede anular (403, RN-05)")
    else:
        print("  omitido: falta la contraseña de la empleada")

    print("\n7. Anulación del ADMIN (HU-15)")
    oil_before = stock(admin, products["DEMO-ACE"]["id"])
    _, voided = admin.call("POST", f"/sales/{first['id']}/void", {"reason": "Cliente devolvió la compra"}, expect=200)
    check(voided["status"] == "VOIDED" and voided["number"] == first["number"], f"{voided['number']} anulada y conserva su número")
    check(float(stock(admin, products["DEMO-ACE"]["id"])) == float(oil_before) + 1, "la anulación devuelve el stock")
    status, again = admin.call("POST", f"/sales/{first['id']}/void", {"reason": "otra vez"})
    check(status == 422, f"no se anula dos veces: {again.get('title') if again else status}")

    print("\n8. Historial del día")
    day = today().isoformat()
    _, history = admin.call("GET", f"/sales?from={day}&to={day}&size=5", expect=200)
    print(f"  {history['totalElements']} ventas hoy; las últimas:")
    for sale in history["content"]:
        state = "ANULADA" if sale["status"] == "VOIDED" else "vigente"
        print(f"  {sale['number']}  {sale['seller']:<26} S/ {sale['total']:>8}  {state}")
    print("\n9. Indicadores del inicio (HU-16)")
    start, end = (today() - timedelta(days=HISTORY_DAYS)).isoformat(), today().isoformat()
    _, dashboard = admin.call("GET", f"/dashboard?from={start}&to={end}", expect=200)
    summary = dashboard["summary"]
    total, count = registered_sales(admin, start, end)
    check(Decimal(summary["total"]) == total and summary["count"] == count,
          f"el total del inicio (S/ {summary['total']}, {summary['count']} ventas) cuadra con las ventas registradas")
    days = sum(1 for d in dashboard["salesByDay"] if d["count"] > 0)
    if args.repartir:
        check(days > HISTORY_DAYS // 2, f"el gráfico tiene ventas en {days} días")
    else:
        print(f"  el gráfico tiene ventas en {days} días (usa --repartir para el último mes)")
    check(len(dashboard["topProducts"]) > 0, f"{len(dashboard['topProducts'])} productos entre los más vendidos")
    codes = {p["productCode"] for p in dashboard["criticalStock"]}
    check(set(CRITICAL) <= codes, f"stock crítico: {', '.join(sorted(codes))}")
    print("\nListo. Abre el inicio y /ventas en el panel para ver los indicadores y el historial.")


if __name__ == "__main__":
    main()
