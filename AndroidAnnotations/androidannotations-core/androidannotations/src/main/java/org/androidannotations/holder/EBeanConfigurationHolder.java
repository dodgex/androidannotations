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
package org.androidannotations.holder;

import static com.helger.jcodemodel.JMod.PUBLIC;
import static com.helger.jcodemodel.JMod.STATIC;
import static org.androidannotations.helper.ModelConstants.generationSuffix;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import org.androidannotations.AndroidAnnotationsEnvironment;

import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JConditional;
import com.helger.jcodemodel.JExpr;
import com.helger.jcodemodel.JFieldVar;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JVar;

public class EBeanConfigurationHolder extends BaseGeneratedClassHolder {

	private JFieldVar singletonField;

	public EBeanConfigurationHolder(AndroidAnnotationsEnvironment environment, TypeElement annotatedElement) throws Exception {
		super(environment, annotatedElement);
	}

	public JMethod createFactoryMethod(ExecutableElement executableElement, boolean hasSingletonScope) {
		TypeMirror returnType = executableElement.getReturnType();
		String methodName = executableElement.getSimpleName().toString();
		AbstractJClass beanClass = codeModelHelper.typeMirrorToJClass(returnType);

		if (hasSingletonScope) {
			JFieldVar beanInstance = generatedClass.field(JMod.PRIVATE, beanClass, methodName + "Instance" + generationSuffix());
			JMethod annotatedMethod = codeModelHelper.overrideAnnotatedMethod(executableElement, this);
			codeModelHelper.removeBody(annotatedMethod);

			JConditional instanceCheck = annotatedMethod.body()._if(beanInstance.eq(JExpr._null()));
			instanceCheck._then().assign(beanInstance, codeModelHelper.getSuperCall(this, annotatedMethod));

			annotatedMethod.body()._return(beanInstance);
		}

		JMethod factoryMethod = generatedClass.method(PUBLIC | STATIC, beanClass, methodName + generationSuffix());
		JVar factoryMethodContextParam = factoryMethod.param(getClasses().CONTEXT, "context");
		JBlock factoryMethodBody = factoryMethod.body();

		JInvocation invocation = invokeOnInstance(methodName);
		if (!executableElement.getParameters().isEmpty()) {
			invocation.arg(factoryMethodContextParam);
		}
		factoryMethodBody._return(invocation);

		return factoryMethod;
	}

	private JInvocation invokeOnInstance(String methodName) {
		if (singletonField == null) {
			singletonField = generatedClass.field(JMod.PRIVATE | JMod.STATIC, generatedClass, "instance" + generationSuffix(), JExpr._new(generatedClass));
		}
		return singletonField.invoke(methodName);
	}

}
