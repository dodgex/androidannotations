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

import static com.helger.jcodemodel.JExpr._new;

import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.annotations.ViewsById;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.helper.CanonicalNameConstants;
import org.androidannotations.helper.IdValidatorHelper;
import org.androidannotations.helper.InjectHelper;
import org.androidannotations.holder.EComponentWithViewSupportHolder;
import org.androidannotations.holder.FoundViewHolder;
import org.androidannotations.holder.HasMethodInjection;
import org.androidannotations.rclass.IRClass;

import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JFieldRef;

public class ViewsByIdHandler extends BaseAnnotationHandler<EComponentWithViewSupportHolder>implements HasMethodInjection<EComponentWithViewSupportHolder> {

	private final InjectHelper<EComponentWithViewSupportHolder> injectHelper;

	public ViewsByIdHandler(AndroidAnnotationsEnvironment environment) {
		super(ViewsById.class, environment);
		injectHelper = new InjectHelper<>(validatorHelper, this, InjectHelper.ValidationMode.VIEW_SUPPORT);
	}

	@Override
	public void validate(Element element, ElementValidation validation) {
		injectHelper.validate(ViewsById.class, element, validation);

		Element param = injectHelper.getParam(element);
		validatorHelper.isDeclaredType(param, validation);

		validatorHelper.extendsListOfView(param, validation);

		validatorHelper.resIdsExist(element, IRClass.Res.ID, IdValidatorHelper.FallbackStrategy.NEED_RES_ID, validation);

		validatorHelper.isNotPrivate(element, validation);
	}

	@Override
	public void process(Element element, EComponentWithViewSupportHolder holder) {
		injectHelper.process(element, holder);
	}

	@Override
	public JBlock getInvocationBlock(EComponentWithViewSupportHolder holder) {
		return holder.getOnViewChangedBody();
	}

	@Override
	public IJExpression getInstanceInvocation(Element element, EComponentWithViewSupportHolder holder, Element param) {
		TypeMirror viewType = extractViewClass(param);
		AbstractJClass viewClass = codeModelHelper.typeMirrorToJClass(viewType);

		String listName = getListName(element, param);
		IJExpression arrayList = instantiateArrayList(viewType, holder, "list_" + listName);
		// TODO remove as soon as blockVirtual() is available in jcodemodel
		holder.getOnViewChangedBodyBeforeFindViews().bracesRequired(false).indentRequired(false);

		List<JFieldRef> idsRefs = annotationHelper.extractAnnotationFieldRefs(element, IRClass.Res.ID, true);
		for (JFieldRef idRef : idsRefs) {
			addViewToListIfNotNull(arrayList, viewClass, idRef, holder);
		}

		return arrayList;
	}

	private String getListName(Element element, Element param) {
		String listName = param.getSimpleName().toString();
		if (element.getKind() == ElementKind.PARAMETER) {
			listName = element.getEnclosingElement().getSimpleName().toString() + "_" + listName;
		} else if (element.getKind() == ElementKind.METHOD) {
			listName = element.getSimpleName().toString() + "_" + listName;
		}
		return listName;
	}

	private IJExpression instantiateArrayList(TypeMirror viewType, EComponentWithViewSupportHolder holder, String name) {
		TypeElement arrayListTypeElement = annotationHelper.typeElementFromQualifiedName(CanonicalNameConstants.ARRAYLIST);
		DeclaredType arrayListType = getProcessingEnvironment().getTypeUtils().getDeclaredType(arrayListTypeElement, viewType);
		AbstractJClass arrayListClass = codeModelHelper.typeMirrorToJClass(arrayListType);

		return holder.getOnViewChangedBodyBeforeFindViews().decl(arrayListClass, name, _new(arrayListClass));
	}

	private TypeMirror extractViewClass(Element element) {
		DeclaredType elementType = (DeclaredType) element.asType();
		List<? extends TypeMirror> elementTypeArguments = elementType.getTypeArguments();

		TypeMirror viewType = annotationHelper.typeElementFromQualifiedName(CanonicalNameConstants.VIEW).asType();
		if (!elementTypeArguments.isEmpty()) {
			viewType = elementTypeArguments.get(0);
		}
		return viewType;
	}

	private void addViewToListIfNotNull(IJExpression elementRef, AbstractJClass viewClass, JFieldRef idRef, EComponentWithViewSupportHolder holder) {
		FoundViewHolder foundViewHolder = holder.getFoundViewHolder(idRef, viewClass);
		foundViewHolder.getIfNotNullBlock().invoke(elementRef, "add").arg(foundViewHolder.getOrCastRef(viewClass));
	}

}
