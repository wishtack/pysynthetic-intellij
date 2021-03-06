package com.wishtack.pysynthetic;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.jetbrains.python.psi.*;
import com.jetbrains.python.psi.resolve.PyResolveContext;
import com.jetbrains.python.psi.types.PyClassTypeImpl;
import com.jetbrains.python.psi.types.PyType;
import com.wishtack.pysynthetic.contracts.ContractNode;
import com.wishtack.pysynthetic.contracts.PyContractsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Created by Jean Hominal on 2016-11-03.
 */
public class SyntheticTypeInfoReader implements CachedValueProvider<SyntheticTypeInfo> {
    private final static SyntheticTypeInfo emptyInfo = new SyntheticTypeInfo(Collections.emptyList(), false);

    @NotNull
    private final PyClass myPyClass;

    public SyntheticTypeInfoReader(@NotNull PyClass pyClass) {
        myPyClass = pyClass;
    }

    @NotNull
    public SyntheticTypeInfo read() {
        return CachedValuesManager.getCachedValue(myPyClass, this);
    }

    @NotNull
    public CachedValueProvider.Result<SyntheticTypeInfo> compute() {
        PyDecoratorList decoratorList = myPyClass.getDecoratorList();

        if (decoratorList == null) {
            return cacheResult(emptyInfo);
        }

        ArrayList<SyntheticMemberInfo> syntheticMemberInfoList = new ArrayList<>();
        boolean withConstructor = false;

        for (PyDecorator d : decoratorList.getDecorators()) {

            SyntheticMemberInfo m = null;

            List<PyCallable> callees = d.multiResolveCalleeFunction(PyResolveContext.defaultContext());
            Optional<PyCallable> callee = callees.stream().filter(Objects::nonNull).findFirst();
            if (!callee.isPresent()) {
                continue;
            }

            String calleeName = callee.get().getQualifiedName();
            if (calleeName == null) {
                continue;
            }

            switch (calleeName) {
                case "synthetic.decorators.synthesize_constructor":
                case "synthetic.decorators.synthesizeConstructor":
                    withConstructor = true;
                    break;
                case "synthetic.decorators.synthesize_property":
                    m = readProperty(d, false);
                    break;
                case "synthetic.decorators.synthesizeProperty":
                    m = readProperty(d, true);
                    break;
                case "synthetic.decorators.synthesize_member":
                    m = readMemberWithAccessors(d, false);
                    break;
                case "synthetic.decorators.synthesizeMember":
                    m = readMemberWithAccessors(d, true);
                    break;
            }

            if (m != null) {
                syntheticMemberInfoList.add(m);
            }
        }

        if (!withConstructor && syntheticMemberInfoList.isEmpty()) {
            return cacheResult(emptyInfo);
        }

        return cacheResult(new SyntheticTypeInfo(Collections.unmodifiableList(syntheticMemberInfoList), withConstructor));
    }

    @NotNull
    private CachedValueProvider.Result<SyntheticTypeInfo> cacheResult(@NotNull SyntheticTypeInfo syntheticTypeInfo) {
        // The dependency argument means that the cache value will be invalidated when the file
        // containing myPyClass is changed.
        return CachedValueProvider.Result.createSingleDependency(syntheticTypeInfo, myPyClass);
    }

    @Nullable
    private SyntheticPropertyMember readProperty(@NotNull PyDecorator decorator, boolean camelCase) {
        StringLiteralExpression memberNameExpression = decorator.getArgument(0, StringLiteralExpression.class);
        if (memberNameExpression == null) return null;

        boolean readOnly = readReadOnlyValue(decorator, camelCase);

        PyContractAnalysisResult contractAnalysis = analyzeContract(decorator);

        PyExpression defaultValue = readDefault(decorator);

        return new SyntheticPropertyMember(myPyClass, decorator, memberNameExpression.getStringValue(), readOnly, contractAnalysis, defaultValue);
    }

    @Nullable
    private SyntheticMemberWithAccessors readMemberWithAccessors(@NotNull PyDecorator decorator, boolean camelCase) {
        StringLiteralExpression memberNameExpression = decorator.getArgument(0, StringLiteralExpression.class);
        if (memberNameExpression == null) return null;
        String memberName = memberNameExpression.getStringValue();

        boolean readOnly = readReadOnlyValue(decorator, camelCase);

        String getterName;
        PyExpression getterNameExpression = decorator.getKeywordArgument(camelCase ? "getterName" : "getter_name");
        if (getterNameExpression == null || !(getterNameExpression instanceof StringLiteralExpression)) {
            getterName = memberName;
        } else {
            getterName = ((StringLiteralExpression)getterNameExpression).getStringValue();
        }

        String setterName = null;
        if (!readOnly) {
            PyExpression setterNameExpression = decorator.getKeywordArgument(camelCase ? "setterName" : "setter_name");
            if (setterNameExpression == null || !(setterNameExpression instanceof StringLiteralExpression)) {
                if (camelCase) {
                    setterName = "set" + Character.toUpperCase(memberName.charAt(0)) + memberName.substring(1);
                } else {
                    setterName = "set_" + memberName;
                }
            } else {
                setterName = ((StringLiteralExpression)setterNameExpression).getStringValue();
            }
        }

        PyContractAnalysisResult contractAnalysis = analyzeContract(decorator);

        PyExpression defaultValue = readDefault(decorator);

        return new SyntheticMemberWithAccessors(myPyClass, decorator, memberName, getterName, setterName, contractAnalysis, defaultValue);
    }

    private static boolean readReadOnlyValue(PyDecorator decorator, boolean camelCase) {
        boolean result = false;
        PyExpression readOnlyExpression = decorator.getKeywordArgument(camelCase ? "readOnly" : "read_only");

        if (readOnlyExpression != null && readOnlyExpression instanceof PyReferenceExpression) {
            result = "True".equals(readOnlyExpression.getName());
        }
        return result;
    }

    private static final PyContractAnalysisResult noContractResult = new PyContractAnalysisResult(null, true);

    @NotNull
    private PyContractAnalysisResult analyzeContract(PyDecorator decorator) {
        PyExpression contractExpression = decorator.getKeywordArgument("contract");

        if (contractExpression instanceof PyReferenceExpression) {
            PyReferenceExpression referenceExpression = (PyReferenceExpression)contractExpression;
            PsiElement referencedElement = referenceExpression.getReference().resolve();

            if (referencedElement instanceof PyClass) {
                PyClass memberClass = (PyClass)referencedElement;
                PyType pyType = new PyClassTypeImpl(memberClass, false);
                return new PyContractAnalysisResult(pyType, false);
            }

            return noContractResult;
        }

        if (contractExpression instanceof StringLiteralExpression) {
            String contract = ((StringLiteralExpression)contractExpression).getStringValue();

            ContractNode parsedContract = PyContractsUtil.parse(contract);

            if (parsedContract != null) {
                PyType computedType = parsedContract.accept(new PyContractsTypeComputer(myPyClass));
                boolean acceptNone = parsedContract.accept(new PyContractsNoneAnalyzer());
                return new PyContractAnalysisResult(computedType, acceptNone);
            }
        }

        return noContractResult;
    }

    @Nullable
    private PyExpression readDefault(PyDecorator decorator) {
        return decorator.getKeywordArgument("default");
    }

}
