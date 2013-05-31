package com.bt.pi.core.entity;

public class DuplicateEntityException extends RuntimeException {

    private static final long serialVersionUID = 5671277561177510412L;

    public DuplicateEntityException(String message) {
        super(message);
    }

}
