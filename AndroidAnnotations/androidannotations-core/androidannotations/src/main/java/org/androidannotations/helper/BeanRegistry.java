package org.androidannotations.helper;

import java.util.HashMap;
import java.util.Map;

import javax.lang.model.type.TypeMirror;

import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMethod;

public class BeanRegistry {
	private Map<String, Entry> beans = new HashMap<>();

	public void registerBean(TypeMirror typeMirror, JDefinedClass generatedClass, JMethod method) {
		beans.put(typeMirror.toString(), new Entry(generatedClass, method));
	}

	public IJExpression getBean(TypeMirror typeMirror, IJExpression contextRef) {
		return beans.get(typeMirror.toString()).invoke(contextRef);
	}

	public boolean hasBean(TypeMirror typeMirror) {
		return beans.containsKey(typeMirror.toString());
	}

	private class Entry {
		JDefinedClass clazz;
		JMethod method;

		Entry(JDefinedClass clazz, JMethod method) {
			this.clazz = clazz;
			this.method = method;
		}

		public JInvocation invoke(IJExpression contextRef) {
			JInvocation invocation = clazz.staticInvoke(method);
			if (!method.params().isEmpty()) {
				invocation.arg(contextRef);
			}
			return invocation;
		}
	}
}
