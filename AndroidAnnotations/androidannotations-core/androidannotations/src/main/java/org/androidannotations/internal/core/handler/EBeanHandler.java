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

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.annotations.EBean;
import org.androidannotations.handler.BaseGeneratingAnnotationHandler;
import org.androidannotations.helper.BeanRegistry;
import org.androidannotations.holder.EBeanHolder;

import com.helger.jcodemodel.AbstractJType;
import com.helger.jcodemodel.JMethod;

public class EBeanHandler extends BaseGeneratingAnnotationHandler<EBeanHolder> {

	private BeanRegistry beanRegistry;

	public EBeanHandler(AndroidAnnotationsEnvironment environment, BeanRegistry beanRegistry) {
		super(EBean.class, environment);
		this.beanRegistry = beanRegistry;
	}

	@Override
	public EBeanHolder createGeneratedClassHolder(AndroidAnnotationsEnvironment environment, TypeElement annotatedComponent) throws Exception {
		return new EBeanHolder(environment, annotatedComponent);
	}

	@Override
	public void validate(Element element, ElementValidation valid) {
		super.validate(element, valid);
		beanRegistry.registerBean(element.asType(), null, null);

		validatorHelper.isNotInterface((TypeElement) element, valid);

		validatorHelper.isNotPrivate(element, valid);

		validatorHelper.isAbstractOrHasEmptyOrContextConstructor(element, valid);
	}

	@Override
	public void process(Element element, EBeanHolder holder) {
		EBean eBeanAnnotation = element.getAnnotation(EBean.class);
		EBean.Scope eBeanScope = eBeanAnnotation.scope();
		boolean hasSingletonScope = eBeanScope == EBean.Scope.Singleton;

		holder.createFactoryMethod(hasSingletonScope);

		if (!hasSingletonScope) {
			holder.invokeInitInConstructor();
			holder.createRebindMethod();
		}

		JMethod method = holder.getGeneratedClass().getMethod(EBeanHolder.GET_INSTANCE_METHOD_NAME, new AbstractJType[] { getClasses().CONTEXT });
		beanRegistry.registerBean(element.asType(), holder.getGeneratedClass(), method);
	}
}
