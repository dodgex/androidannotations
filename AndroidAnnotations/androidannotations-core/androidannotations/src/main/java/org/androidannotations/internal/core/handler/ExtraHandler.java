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

import static com.helger.jcodemodel.JExpr.invoke;
import static com.helger.jcodemodel.JExpr.lit;
import static com.helger.jcodemodel.JMod.FINAL;
import static com.helger.jcodemodel.JMod.PUBLIC;
import static com.helger.jcodemodel.JMod.STATIC;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.annotations.Extra;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.helper.BundleHelper;
import org.androidannotations.helper.CaseHelper;
import org.androidannotations.helper.InjectHelper;
import org.androidannotations.holder.HasExtras;
import org.androidannotations.holder.HasIntentBuilder;
import org.androidannotations.holder.HasMethodInjection;

import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JExpr;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JFieldVar;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JOp;
import com.helger.jcodemodel.JVar;

public class ExtraHandler extends BaseAnnotationHandler<HasExtras>implements HasMethodInjection<HasExtras> {

	private final InjectHelper<HasExtras> injectHelper;

	public ExtraHandler(AndroidAnnotationsEnvironment environment) {
		super(Extra.class, environment);
		injectHelper = new InjectHelper<>(validatorHelper, this, InjectHelper.ValidationMode.ACTIVITY);
	}

	@Override
	public void validate(Element element, ElementValidation validation) {
		/*
		 * TODO since we override setIntent(), we should check that the
		 * setIntent() method can be overridden
		 */

		injectHelper.validate(Extra.class, element, validation);

		validatorHelper.isNotPrivate(element, validation);

		Element param = injectHelper.getParam(element);
		validatorHelper.canBePutInABundle(param, validation);
	}

	@Override
	public void process(Element element, HasExtras holder) {
		injectHelper.process(element, holder);
	}

	@Override
	public JBlock getInvocationBlock(HasExtras holder) {
		return holder.getInjectExtrasBlock();
	}

	@Override
	public IJExpression getInstanceInvocation(Element element, HasExtras holder, Element param) {
		Extra annotation = element.getAnnotation(Extra.class);
		String extraKey = annotation.value();
		String fieldName = element.getSimpleName().toString();
		if (extraKey.isEmpty()) {
			extraKey = fieldName;
		}

		JFieldVar extraKeyStaticField = createStaticExtraField(holder, extraKey, fieldName);
		if (holder instanceof HasIntentBuilder) {
			String docComment = getProcessingEnvironment().getElementUtils().getDocComment(element);
			createIntentInjectionMethod(param, (HasIntentBuilder) holder, extraKeyStaticField, fieldName, docComment);
		}
		return injectExtraInComponent(param, holder, extraKeyStaticField, fieldName);
	}

	private JFieldVar createStaticExtraField(HasExtras holder, String extraKey, String fieldName) {
		String staticFieldName = CaseHelper.camelCaseToUpperSnakeCase(null, fieldName, "Extra");
		JFieldVar staticExtraField = holder.getGeneratedClass().fields().get(staticFieldName);
		if (staticExtraField == null) {
			staticExtraField = holder.getGeneratedClass().field(PUBLIC | STATIC | FINAL, getClasses().STRING, staticFieldName, lit(extraKey));
		}
		return staticExtraField;
	}

	private IJExpression injectExtraInComponent(Element element, HasExtras holder, JFieldVar extraKeyStaticField, String fieldName) {
		JMethod injectExtrasMethod = holder.getInjectExtrasMethod();
		JVar extras = holder.getInjectExtras();

		DeclaredType enclosingClassType;
		if (element.getKind() == ElementKind.PARAMETER) {
			enclosingClassType = (DeclaredType) element.getEnclosingElement().getEnclosingElement().asType();
		} else {
			enclosingClassType = (DeclaredType) element.getEnclosingElement().asType();
		}
		TypeMirror type = codeModelHelper.getActualType(element, enclosingClassType, holder);
		AbstractJClass elementClass = codeModelHelper.typeMirrorToJClass(element.asType());
		BundleHelper bundleHelper = new BundleHelper(getEnvironment(), type);

		IJExpression intent = invoke("getIntent");
		IJExpression restoreMethodCall = bundleHelper.getExpressionToRestoreFromIntentOrBundle(elementClass, intent, extras, extraKeyStaticField, injectExtrasMethod);

		if (element.getKind() == ElementKind.FIELD) {
			JFieldRef extraField = JExpr.ref(fieldName);
			return JOp.cond(JExpr.invoke(extras, "containsKey").arg(extraKeyStaticField), restoreMethodCall, extraField);
		} else {
			return restoreMethodCall;
		}
	}

	private void createIntentInjectionMethod(Element element, HasIntentBuilder holder, JFieldVar extraKeyStaticField, String fieldName, String docComment) {
		holder.getIntentBuilder().getPutExtraMethod(element.asType(), fieldName, extraKeyStaticField, docComment);
	}
}
