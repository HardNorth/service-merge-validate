/*
 * Copyright (C) 2019 Epic Games, Inc. All Rights Reserved.
 */

package net.hardnorth.github.merge.context;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
public class NoContentControllerAdvice implements ResponseBodyAdvice<Void> {

	@Override
	public boolean supports(@NonNull MethodParameter returnType, @Nullable Class<? extends HttpMessageConverter<?>> converterType) {
		return returnType.getParameterType().isAssignableFrom(void.class);

	}

	@Override
	public Void beforeBodyWrite(Void body, @NonNull MethodParameter returnType, @Nullable MediaType mediaType,
			@Nullable Class<? extends HttpMessageConverter<?>> converterType, @Nullable ServerHttpRequest request,
			@NonNull ServerHttpResponse response) {
		if (returnType.getParameterType().isAssignableFrom(void.class)) {
			response.setStatusCode(HttpStatus.NO_CONTENT);
		}
		return body;
	}
}
