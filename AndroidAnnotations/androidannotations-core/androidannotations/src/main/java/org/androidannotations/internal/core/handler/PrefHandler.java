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

import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.androidannotations.annotations.sharedpreferences.SharedPref;
import org.androidannotations.helper.InjectHelper;
import org.androidannotations.holder.EComponentHolder;
import org.androidannotations.holder.GeneratedClassHolder;
import org.androidannotations.holder.HasMethodInjection;

import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JExpr;

public class PrefHandler extends CoreBaseAnnotationHandler<EComponentHolder>implements HasMethodInjection<EComponentHolder> {

	private final InjectHelper<EComponentHolder> injectHelper;

	public PrefHandler(AndroidAnnotationsEnvironment environment) {
		super(Pref.class, environment);
		injectHelper = new InjectHelper<>(validatorHelper, this);
	}

	@Override
	public void validate(Element element, ElementValidation validation) {
		injectHelper.validate(Pref.class, element, validation);

		validatorHelper.isNotPrivate(element, validation);

		coreValidatorHelper.isSharedPreference(element, validation);
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
		TypeMirror fieldTypeMirror = param.asType();
		AbstractJClass prefClass = getJClass(fieldTypeMirror.toString());

		String elementTypeName = fieldTypeMirror.toString();
		int index = elementTypeName.lastIndexOf(".");
		if (index != -1) {
			elementTypeName = elementTypeName.substring(index + 1);
		}

		Set<? extends Element> sharedPrefElements = getEnvironment().getValidatedElements().getRootAnnotatedElements(SharedPref.class.getName());
		for (Element sharedPrefElement : sharedPrefElements) {
			GeneratedClassHolder sharedPrefHolder = getEnvironment().getGeneratedClassHolder(sharedPrefElement);
			String sharedPrefName = sharedPrefHolder.getGeneratedClass().name();

			if (elementTypeName.equals(sharedPrefName)) {
				prefClass = sharedPrefHolder.getGeneratedClass();
				break;
			}
		}

		return JExpr._new(prefClass).arg(holder.getContextRef());
	}
}
