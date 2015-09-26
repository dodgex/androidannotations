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
package org.androidannotations.internal.core.handler;

import static com.helger.jcodemodel.JExpr.cast;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.helper.CanonicalNameConstants;
import org.androidannotations.helper.InjectHelper;
import org.androidannotations.holder.EBeanHolder;
import org.androidannotations.holder.HasMethodInjection;

import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JExpr;
import com.helger.jcodemodel.JOp;

public class RootContextHandler extends BaseAnnotationHandler<EBeanHolder>implements HasMethodInjection<EBeanHolder> {

	private final InjectHelper<EBeanHolder> injectHelper;

	public RootContextHandler(AndroidAnnotationsEnvironment environment) {
		super(RootContext.class, environment);
		injectHelper = new InjectHelper<>(validatorHelper, this, InjectHelper.ValidationMode.BEAN);
	}

	@Override
	public void validate(Element element, ElementValidation validation) {
		injectHelper.validate(RootContext.class, element, validation);

		Element param = injectHelper.getParam(element);
		validatorHelper.extendsContext(param, validation);

		validatorHelper.isNotPrivate(element, validation);
	}

	@Override
	public void process(Element element, EBeanHolder holder) {
		injectHelper.process(element, holder);
	}

	@Override
	public JBlock getInvocationBlock(EBeanHolder holder) {
		return holder.getInitBody();
	}

	@Override
	public IJExpression getInstanceInvocation(Element element, EBeanHolder holder, Element param) {
		TypeMirror elementType = param.asType();
		String typeQualifiedName = elementType.toString();

		IJExpression contextRef = holder.getContextRef();

		if (CanonicalNameConstants.CONTEXT.equals(typeQualifiedName)) {
			return contextRef;
		} else {
			AbstractJClass extendingContextClass = getEnvironment().getJClass(typeQualifiedName);
			return JOp.cond(holder.getContextRef()._instanceof(extendingContextClass), cast(extendingContextClass, holder.getContextRef()), JExpr._null());
		}
	}
}
