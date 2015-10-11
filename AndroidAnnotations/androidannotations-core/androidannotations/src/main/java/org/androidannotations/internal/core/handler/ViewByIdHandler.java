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

import static com.helger.jcodemodel.JExpr.ref;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.helper.IdValidatorHelper;
import org.androidannotations.helper.InjectHelper;
import org.androidannotations.holder.EComponentWithViewSupportHolder;
import org.androidannotations.holder.EFragmentHolder;
import org.androidannotations.holder.HasMethodInjection;
import org.androidannotations.rclass.IRClass;

import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JFieldRef;

public class ViewByIdHandler extends BaseAnnotationHandler<EComponentWithViewSupportHolder>implements HasMethodInjection<EComponentWithViewSupportHolder> {

	private final InjectHelper<EComponentWithViewSupportHolder> injectHelper;

	public ViewByIdHandler(AndroidAnnotationsEnvironment environment) {
		super(ViewById.class, environment);
		injectHelper = new InjectHelper<>(validatorHelper, this, InjectHelper.ValidationMode.VIEW_SUPPORT);
	}

	@Override
	public void validate(Element element, ElementValidation validation) {
		injectHelper.validate(ViewById.class, element, validation);

		Element param = injectHelper.getParam(element);
		validatorHelper.isDeclaredType(param, validation);

		validatorHelper.extendsView(param, validation);

		validatorHelper.resIdsExist(element, IRClass.Res.ID, IdValidatorHelper.FallbackStrategy.USE_ELEMENT_NAME, validation);

		validatorHelper.isNotPrivate(element, validation);
	}

	@Override
	public void process(Element element, EComponentWithViewSupportHolder holder) {
		injectHelper.process(element, holder);
		if (holder instanceof EFragmentHolder) {
			String fieldName = element.getSimpleName().toString();
			((EFragmentHolder) holder).clearInjectedView(ref(fieldName));
		}
	}

	@Override
	public JBlock getInvocationBlock(EComponentWithViewSupportHolder holder) {
		return holder.getOnViewChangedBody();
	}

	@Override
	public IJExpression getInstanceInvocation(Element element, EComponentWithViewSupportHolder holder, Element param) {
		TypeMirror uiFieldTypeMirror = param.asType();

		JFieldRef idRef = annotationHelper.extractOneAnnotationFieldRef(element, IRClass.Res.ID, true);
		AbstractJClass viewClass = codeModelHelper.typeMirrorToJClass(uiFieldTypeMirror);

		return holder.getFoundViewHolder(idRef, viewClass).getOrCastRef(viewClass);
	}
}
