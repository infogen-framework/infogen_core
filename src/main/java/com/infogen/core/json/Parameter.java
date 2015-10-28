package com.infogen.core.json;

import java.util.IdentityHashMap;

/**
 * HTTP协议调用端输入参数类
 * 
 * @author larry/larrylv@outlook.com/创建时间 2015年6月12日 下午3:56:17
 * @since 1.0
 * @version 1.0
 */
public class Parameter extends IdentityHashMap<String, String> {
	private static final long serialVersionUID = -5436768657673377874L;

	public static Parameter create() {
		return new Parameter();
	}

	public static Parameter create(String key, String value) {
		return new Parameter().add(key, value);
	}

	public Parameter add(String key, String value) {
		super.put(key, value);
		return this;
	}

	@Override
	public String put(String key, String value) {
		super.put(key, value);
		return value;
	}
}
