package com.kardexa.identity.domain.exception;

import com.kardexa.shared.domain.ForbiddenException;

/** El registro inicial solo existe mientras no haya ningún usuario. */
public class SetupAlreadyDoneException extends ForbiddenException {

    public SetupAlreadyDoneException() {
        super("RF-01", "El registro inicial ya se hizo. Pide a un administrador que cree tu cuenta.");
    }

    @Override
    public String title() {
        return "Registro inicial cerrado";
    }
}
