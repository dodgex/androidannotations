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

import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.helper.CanonicalNameConstants;
import org.androidannotations.holder.EComponentHolder;
import org.androidannotations.internal.core.model.AndroidRes;

import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JOp;
import com.helger.jcodemodel.JVar;

public class DrawableResHandler extends AbstractResHandler {

	private static final int MIN_SDK_WITH_CONTEXT_GET_DRAWABLE = 21;

	public DrawableResHandler(AndroidAnnotationsEnvironment environment) {
		super(AndroidRes.DRAWABLE, environment);
	}

	@Override
	protected IJExpression getInstanceInvocation(EComponentHolder holder, JFieldRef idRef) {
		if (hasContextCompatInClasspath()) {
			return getClasses().CONTEXT_COMPAT.staticInvoke("getDrawable").arg(holder.getContextRef()).arg(idRef);
		} else if (shouldUseContextGetDrawableMethod() && !hasContextCompatInClasspath()) {
			return holder.getContextRef().invoke("getDrawable").arg(idRef);
		} else if (!shouldUseContextGetDrawableMethod() && hasGetDrawableInContext() && !hasContextCompatInClasspath()) {
			return createCallWithIfGuard(holder, idRef);
		} else {
			return invoke(holder.getResourcesRef(), androidRes.getResourceMethodName()).arg(idRef);
		}
	}

	private boolean hasContextCompatInClasspath() {
		return getProcessingEnvironment().getElementUtils().getTypeElement(CanonicalNameConstants.CONTEXT_COMPAT) != null;
	}

	private boolean shouldUseContextGetDrawableMethod() {
		return getEnvironment().getAndroidManifest().getMinSdkVersion() >= MIN_SDK_WITH_CONTEXT_GET_DRAWABLE;
	}

	private boolean hasGetDrawableInContext() {
		TypeElement context = getProcessingEnvironment().getElementUtils().getTypeElement(CanonicalNameConstants.CONTEXT);

		return hasGetDrawable(context);
	}

	private IJExpression createCallWithIfGuard(EComponentHolder holder, JFieldRef idRef) {
		JVar resourcesRef = holder.getResourcesRef();
		IJExpression buildVersionCondition = getClasses().BUILD_VERSION.staticRef("SDK_INT").gte(getClasses().BUILD_VERSION_CODES.staticRef("LOLLIPOP"));

		return JOp.cond(buildVersionCondition, holder.getContextRef().invoke("getDrawable").arg(idRef), resourcesRef.invoke("getDrawable").arg(idRef));
	}

	private boolean hasGetDrawable(TypeElement type) {
		if (type == null) {
			return false;
		}

		List<? extends Element> allMembers = getProcessingEnvironment().getElementUtils().getAllMembers(type);
		for (ExecutableElement element : ElementFilter.methodsIn(allMembers)) {
			if (element.getSimpleName().contentEquals("getDrawable")) {
				return true;
			}
		}
		return false;
	}
}
