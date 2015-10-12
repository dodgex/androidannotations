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
import javax.lang.model.type.TypeMirror;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.annotations.Bean;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.helper.BeanRegistry;
import org.androidannotations.helper.InjectHelper;
import org.androidannotations.holder.EComponentHolder;
import org.androidannotations.holder.HasMethodInjection;

import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JBlock;

public class BeanHandler extends BaseAnnotationHandler<EComponentHolder>implements HasMethodInjection<EComponentHolder> {

	private final InjectHelper<EComponentHolder> injectHelper;
	private BeanRegistry beanRegistry;

	public BeanHandler(AndroidAnnotationsEnvironment environment, BeanRegistry beanRegistry) {
		super(Bean.class, environment);
		this.beanRegistry = beanRegistry;
		injectHelper = new InjectHelper<>(validatorHelper, this);
	}

	@Override
	public void validate(Element element, ElementValidation validation) {
		injectHelper.validate(Bean.class, element, validation);

		Element param = injectHelper.getParam(element);
		if (!beanRegistry.hasBean(param.asType())) {
			validation.addError(element, "No EBean for " + param.asType() + " registred.");
		}

		validatorHelper.isNotPrivate(element, validation);
	}

	@Override
	public void process(Element element, EComponentHolder holder) {
		injectHelper.process(element, holder);
	}

	@Override
	public JBlock getInvocationBlock(EComponentHolder holder) {
		return holder.getInitBody();
	}

	@Override
	public IJExpression getInstanceInvocation(Element element, EComponentHolder holder, Element param) {
		TypeMirror typeMirror = annotationHelper.extractAnnotationClassParameter(element);
		if (typeMirror == null) {
			typeMirror = param.asType();
			typeMirror = getProcessingEnvironment().getTypeUtils().erasure(typeMirror);
		}
		return beanRegistry.getBean(typeMirror, holder.getContextRef());
	}
}
