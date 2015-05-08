/**
 * 
 */
package com.infogen.aop.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Set;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;

/**
 * @author larry/larrylv@outlook.com/创建时间 2015年2月27日 上午11:47:39
 * @since 1.0
 * @version 1.0
 */
public class InfoGen_Transformer implements ClassFileTransformer {

	private InfoGen_Agent_Advice_Class infogen_advice = null;
	private Class<?> reload_class = null;
	private ClassPool class_pool = ClassPool.getDefault();

	public InfoGen_Transformer(InfoGen_Agent_Advice_Class infogen_advice, Class<?> reload_class) {
		this.infogen_advice = infogen_advice;
		this.reload_class = reload_class;
	}

	// @com.infogen.aop.annotation.Execution
	// @com.infogen.aop.annotation.Execution()

	// @com.infogen.aop.annotation.Invoke
	// @com.infogen.aop.annotation.Invoke(value=)

	// @com.infogen.aop.annotation.Execution(value="asdasdasdas", value1="")
	// @com.infogen.aop.annotation.Execution(value1=, value=asdasdasdas)

	// @com.infogen.aop.annotation.Execution(value="asdasdasdas")
	// @com.infogen.aop.annotation.Execution(value1=, value=asdasdasdas)
	private boolean annotation_check(String byjava, String byjavassist) {
		byjava = byjava.replaceAll(" ", "");
		byjavassist = byjavassist.replaceAll(" ", "").replaceAll("\"", "");

		int indexOf_start_byjava = byjava.indexOf("(");
		int indexOf_end_byjava = byjava.indexOf(")");
		int indexOf_start_byjavassist = byjavassist.indexOf("(");

		String annotation_name_byjava = byjava.substring(0, indexOf_start_byjava);
		String annotation_name_byjavassist = indexOf_start_byjavassist == -1 ? byjavassist : byjavassist.substring(0, indexOf_start_byjavassist);
		if (!annotation_name_byjava.equals(annotation_name_byjavassist)) {
			return false;
		}

		for (String string : byjava.substring(indexOf_start_byjava + 1, indexOf_end_byjava).split(",")) {
			if (string.split("=").length == 2 && !byjavassist.contains(string)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		if (!className.equals(infogen_advice.getClass_name().replace(".", "/"))) {
			return classfileBuffer;
		}
		String class_name = infogen_advice.getClass_name();
		try {
			class_pool.insertClassPath(new ClassClassPath(reload_class));// war包下使用必须
			CtClass ct_class = class_pool.get(class_name);
			ct_class.defrost();

			//
			Set<InfoGen_Agent_Advice_Field> fields = infogen_advice.getFields();
			for (InfoGen_Agent_Advice_Field infoGen_Agent_Advice_Field : fields) {
				String insertAfter = new StringBuilder("this.").append(infoGen_Agent_Advice_Field.getField_name()).append(" = ").append(infoGen_Agent_Advice_Field.getValue()).toString();
				CtConstructor[] constructors = ct_class.getConstructors();
				for (CtConstructor ctConstructor : constructors) {
					ctConstructor.insertAfter(insertAfter);
				}
			}

			//
			Set<InfoGen_Agent_Advice_Method> methods = infogen_advice.getMethods();
			for (InfoGen_Agent_Advice_Method infogen_agent_advice_method : methods) {
				CtMethod[] declaredMethods = ct_class.getDeclaredMethods(infogen_agent_advice_method.getMethod_name());
				for (CtMethod ct_method : declaredMethods) {
					Object[] annotations = ct_method.getAnnotations();
					for (Object object : annotations) {
						if (!annotation_check(infogen_agent_advice_method.getAnnotation(), object.toString())) {
							continue;
						}

						String long_local_variable = infogen_agent_advice_method.getLong_local_variable();
						if (long_local_variable != null) {
							ct_method.addLocalVariable(long_local_variable, CtClass.longType);
						}
						String insert_before = infogen_agent_advice_method.getInsert_before();
						if (insert_before != null) {
							ct_method.insertBefore(insert_before);
						}
						String insert_after = infogen_agent_advice_method.getInsert_after();
						if (insert_after != null) {
							ct_method.insertAfter(insert_after);
						}
						String add_catch = infogen_agent_advice_method.getAdd_catch();
						if (add_catch != null) {
							CtClass ctClass = class_pool.get("java.lang.Throwable");
							ct_method.addCatch(add_catch, ctClass);
						}
						break;
					}
				}
			}

			return ct_class.toBytecode();
		} catch (Throwable e) {
			System.out.println("transform 字节码文件错误 :");
			e.printStackTrace();
			return classfileBuffer;
		}

	}
}