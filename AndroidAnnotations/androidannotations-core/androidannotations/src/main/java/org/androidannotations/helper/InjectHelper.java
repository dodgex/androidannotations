/**
 * Copyright (C) 2010-2015 eBusiness Information, Excilys Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed To in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.androidannotations.helper;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import org.androidannotations.ElementValidation;
import org.androidannotations.holder.GeneratedClassHolder;
import org.androidannotations.holder.HasMethodInjection;
import org.androidannotations.holder.HasMethodInjectionWrapper;

import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.IJStatement;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JExpr;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JInvocation;

public class InjectHelper<T extends GeneratedClassHolder> {

	public enum ValidationMode {
		ENHANCED, VIEW_SUPPORT, BEAN, ACTIVITY_OR_FRAGMENT, ACTIVITY, FRAGMENT;
	}

	private final Map<ExecutableElement, List<ParamHelper>> methodParameterMap = new HashMap<>();

	private final ValidatorHelper validatorHelper;
	private final HasMethodInjection<T> handler;
	private ValidationMode validationMode;

	public InjectHelper(ValidatorHelper validatorHelper, HasMethodInjection<T> handler) {
		this(validatorHelper, handler, ValidationMode.ENHANCED);
	}

	public InjectHelper(ValidatorHelper validatorHelper, HasMethodInjection<T> handler, ValidationMode validationMode) {
		this.validatorHelper = validatorHelper;
		this.validationMode = validationMode;
		this.handler = handler;
	}

	public void validate(Class<? extends Annotation> expectedAnnotation, Element element, ElementValidation valid) {
		Element enclosingElement = element.getEnclosingElement();
		if (element instanceof VariableElement && enclosingElement instanceof ExecutableElement) {
			validatorHelper.param.annotatedWith(expectedAnnotation).multiple().validate((ExecutableElement) enclosingElement, valid);
			validatorHelper.doesNotHaveAnyOfSupportedAnnotations(enclosingElement, valid);
			validateEnclosingElement(enclosingElement, valid);

		} else if (element instanceof ExecutableElement) {
			validateEnclosingElement(element, valid);
			validatorHelper.param.anyType().validate((ExecutableElement) element, valid);
			List<? extends VariableElement> parameters = ((ExecutableElement) element).getParameters();
			for (VariableElement param : parameters) {
				validatorHelper.doesNotHaveAnyOfSupportedAnnotations(param, valid);
			}

		} else {
			validateEnclosingElement(element, valid);
		}
	}

	public Element getParam(Element element) {
		if (element instanceof ExecutableElement) {
			return ((ExecutableElement) element).getParameters().get(0);
		}
		return element;
	}

	public void process(Element element, T holder) {
		if (element instanceof ExecutableElement) {
			processMethod(element, holder);
		} else {
			Element enclosingElement = element.getEnclosingElement();
			if (enclosingElement instanceof ExecutableElement) {
				processParam(element, holder);
			} else {
				processField(element, holder);
			}
		}
	}

	private void validateEnclosingElement(Element element, ElementValidation valid) {
		switch (validationMode) {
		case ENHANCED:
			validatorHelper.enclosingElementHasEnhancedComponentAnnotation(element, valid);
			break;
		case VIEW_SUPPORT:
			validatorHelper.enclosingElementHasEnhancedViewSupportAnnotation(element, valid);
			break;
		case BEAN:
			validatorHelper.enclosingElementHasEBeanAnnotation(element, valid);
			break;
		case ACTIVITY_OR_FRAGMENT:
			validatorHelper.enclosingElementHasEActivityOrEFragment(element, valid);
			break;
		case ACTIVITY:
			validatorHelper.enclosingElementHasEActivity(element, valid);
			break;
		case FRAGMENT:
			validatorHelper.enclosingElementHasEFragment(element, valid);
			break;
		}
	}

	private void processParam(Element element, T holder) {
		ExecutableElement method = (ExecutableElement) element.getEnclosingElement();
		List<? extends VariableElement> parameters = method.getParameters();
		List<ParamHelper> parameterList = methodParameterMap.get(method);
		int paramCount = parameters.size();

		if (parameterList == null) {
			parameterList = new ArrayList<>();
			methodParameterMap.put(method, parameterList);
		}

		for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
			VariableElement param = parameters.get(paramIndex);
			if (param.equals(element)) {
				IJExpression getInstance = handler.getInstanceInvocation(param, holder, param);
				parameterList.add(new ParamHelper(getInstance, paramIndex));
			}
		}

		if (parameterList.size() == paramCount) {
			String methodName = method.getSimpleName().toString();

			Collections.sort(parameterList);

			JInvocation invocation = JExpr.invoke(methodName);
			for (ParamHelper parameter : parameterList) {
				invocation.arg(parameter.beanInstance);
			}
			emitInjection(holder, invocation);

			methodParameterMap.remove(method);
		}
	}

	private void processField(Element element, T holder) {
		String fieldName = element.getSimpleName().toString();
		JFieldRef beanField = JExpr._this().ref(fieldName);

		IJExpression getInstance = handler.getInstanceInvocation(element, holder, element);
		emitInjection(holder, JExpr.assign(beanField, getInstance));
	}

	private void processMethod(Element element, T holder) {
		ExecutableElement executableElement = (ExecutableElement) element;
		VariableElement param = executableElement.getParameters().get(0);
		String methodName = executableElement.getSimpleName().toString();

		IJExpression getInstance = handler.getInstanceInvocation(element, holder, param);
		emitInjection(holder, JExpr.invoke(methodName).arg(getInstance));
	}

	private void emitInjection(T holder, IJStatement injectStatement) {
		JBlock block = handler.getInvocationBlock(holder);
		if (handler instanceof HasMethodInjectionWrapper<?>) {
			((HasMethodInjectionWrapper<T>) handler).wrapInjection(holder, block, injectStatement);
		} else {
			block.add(injectStatement);
		}
	}

	private static class ParamHelper implements Comparable<ParamHelper> {
		private final int argumentOrder;
		private final IJExpression beanInstance;

		ParamHelper(IJExpression beanInstance, int argumentOrder) {
			this.beanInstance = beanInstance;
			this.argumentOrder = argumentOrder;
		}

		@Override
		public int compareTo(ParamHelper o) {
			return this.argumentOrder - o.argumentOrder;
		}
	}
}
