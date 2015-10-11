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
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.annotations.BeanConfiguration;
import org.androidannotations.annotations.EBeanConfiguration;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.helper.BeanRegistry;
import org.androidannotations.helper.CanonicalNameConstants;
import org.androidannotations.holder.EBeanConfigurationHolder;

import com.helger.jcodemodel.JMethod;

public class BeanConfigurationHandler extends BaseAnnotationHandler<EBeanConfigurationHolder> {

	private BeanRegistry beanRegistry;

	public BeanConfigurationHandler(AndroidAnnotationsEnvironment environment, BeanRegistry beanRegistry) {
		super(BeanConfiguration.class, environment);
		this.beanRegistry = beanRegistry;
	}

	@Override
	public void validate(Element element, ElementValidation valid) {
		validatorHelper.enclosingElementHasAnnotation(EBeanConfiguration.class, element, valid);

		ExecutableElement executableElement = (ExecutableElement) element;
		if (beanRegistry.hasBean(executableElement.getReturnType())) {
			valid.addError(executableElement, "There is already a bean for " + executableElement.getReturnType() + " registred.");
		}
		beanRegistry.registerBean(executableElement.getReturnType(), null, null);

		validatorHelper.param.type(CanonicalNameConstants.CONTEXT).optional().validate(executableElement, valid);

		validatorHelper.returnTypeIsNotVoid(executableElement, valid);

		validatorHelper.isNotPrivate(element, valid);

		validatorHelper.isNotFinal(element, valid);
	}

	@Override
	public void process(Element element, EBeanConfigurationHolder holder) {
		ExecutableElement executableElement = (ExecutableElement) element;
		TypeMirror returnType = executableElement.getReturnType();

		boolean isSingleton = annotationHelper.extractAnnotationParameter(executableElement, BeanConfiguration.class.getName(), "singleton");

		JMethod method = holder.createFactoryMethod(executableElement, isSingleton);

		beanRegistry.registerBean(returnType, holder.getGeneratedClass(), method);
	}
}
