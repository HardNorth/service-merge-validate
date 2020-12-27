/*
 * Copyright (C) 2019 Epic Games, Inc. All Rights Reserved.
 */

package net.hardnorth.github.merge.web;

public class ConnectionException extends RuntimeException
{
    public ConnectionException(String message)
    {
        super(message);
    }

    public ConnectionException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
