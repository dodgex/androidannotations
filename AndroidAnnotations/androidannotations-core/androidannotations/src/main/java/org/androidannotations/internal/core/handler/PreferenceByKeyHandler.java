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
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeMirror;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.annotations.PreferenceByKey;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.helper.IdValidatorHelper;
import org.androidannotations.helper.InjectHelper;
import org.androidannotations.holder.HasMethodInjection;
import org.androidannotations.holder.HasPreferences;
import org.androidannotations.rclass.IRClass;

import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JFieldRef;

public class PreferenceByKeyHandler extends BaseAnnotationHandler<HasPreferences>implements HasMethodInjection<HasPreferences> {

	private final InjectHelper<HasPreferences> injectHelper;

	public PreferenceByKeyHandler(AndroidAnnotationsEnvironment environment) {
		super(PreferenceByKey.class, environment);
		injectHelper = new InjectHelper<>(validatorHelper, this, InjectHelper.ValidationMode.ACTIVITY_OR_FRAGMENT);
	}

	@Override
	protected void validate(Element element, ElementValidation valid) {
		injectHelper.validate(PreferenceByKey.class, element, valid);

		if (element.getKind() == ElementKind.PARAMETER) {
			validatorHelper.enclosingElementExtendsPreferenceActivityOrPreferenceFragment(element.getEnclosingElement(), valid);
		} else {
			validatorHelper.enclosingElementExtendsPreferenceActivityOrPreferenceFragment(element, valid);
		}

		Element param = injectHelper.getParam(element);
		validatorHelper.isDeclaredType(param, valid);

		validatorHelper.extendsPreference(param, valid);

		validatorHelper.isNotPrivate(element, valid);

		validatorHelper.resIdsExist(element, IRClass.Res.STRING, IdValidatorHelper.FallbackStrategy.USE_ELEMENT_NAME, valid);
	}

	@Override
	public void process(Element element, HasPreferences holder) throws Exception {
		injectHelper.process(element, holder);
	}

	@Override
	public JBlock getInvocationBlock(HasPreferences holder) {
		return holder.getAddPreferencesFromResourceBlock();
	}

	@Override
	public IJExpression getInstanceInvocation(Element element, HasPreferences holder, Element param) {
		TypeMirror prefFieldTypeMirror = param.asType();
		String typeQualifiedName = prefFieldTypeMirror.toString();

		JFieldRef idRef = annotationHelper.extractOneAnnotationFieldRef(element, IRClass.Res.STRING, true);
		AbstractJClass preferenceClass = getJClass(typeQualifiedName);

		return holder.getFoundPreferenceHolder(idRef, preferenceClass).getOrCastRef(preferenceClass);
	}

}
