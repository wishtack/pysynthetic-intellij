package com.wishtack.pysynthetic.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.PlatformIcons;
import com.jetbrains.python.PyElementTypes;
import com.jetbrains.python.psi.*;
import com.jetbrains.python.psi.stubs.PyFunctionStub;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Collections;
import java.util.List;

/**
 * This class implements boring parts of PyFunction that synthetic accessors have only trivial responses for.
 *
 * @author Jean Hominal
 */
public abstract class AbstractAccessor extends ASTWrapperPsiElement implements PyFunction {

    AbstractAccessor(@NotNull ASTNode node) {
        super(node);
    }

    @Nullable
    @Override
    public final ASTNode getNameNode() {
        return null;
    }

    @Nullable
    @Override
    public final String getDeprecationMessage() {
        return null;
    }

    @Nullable
    @Override
    public final Modifier getModifier() {
        return null;
    }

    @Override
    public final boolean isAsync() {
        return false;
    }

    @Override
    public final boolean isAsyncAllowed() {
        return false;
    }

    @Override
    public final boolean isGenerator() {
        return false;
    }

    @Nullable
    @Override
    public final Property getProperty() {
        return null;
    }

    @Nullable
    @Override
    public final PyAnnotation getAnnotation() {
        return null;
    }

    @Nullable
    @Override
    public final String getAnnotationValue() {
        return null;
    }

    @NotNull
    @Override
    public final List<PyAssignmentStatement> findAttributes() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public final ProtectionLevel getProtectionLevel() {
        return ProtectionLevel.PUBLIC;
    }

    @Nullable
    @Override
    public final PsiElement getNameIdentifier() {
        return null;
    }

    @Override
    public final IStubElementType getElementType() {
        return PyElementTypes.FUNCTION_DECLARATION;
    }

    @Override
    public final PyFunctionStub getStub() {
        return null;
    }

    @Nullable
    @Override
    public final PyDecoratorList getDecoratorList() {
        return null;
    }

    @Nullable
    @Override
    public final String getDocStringValue() {
        return null;
    }

    @Nullable
    @Override
    public final StructuredDocString getStructuredDocString() {
        return null;
    }

    @Nullable
    @Override
    public final PyStringLiteralExpression getDocStringExpression() {
        return null;
    }

    @NotNull
    @Override
    public final PyStatementList getStatementList() {
        throw new UnsupportedOperationException("No statement list. Sue me.");
    }

    @Nullable
    @Override
    public final PsiComment getTypeComment() {
        return null;
    }

    @Nullable
    @Override
    public final String getTypeCommentAnnotation() {
        return null;
    }

    @Override
    public final PyFunction asMethod() {
        return this;
    }

    @Override
    public final PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
        return null;
    }

    @Nullable
    @Override
    public final Icon getIcon(int flags) {
        return PlatformIcons.METHOD_ICON;
    }

    @Override
    public boolean onlyRaisesNotImplementedError() {
        return false;
    }
}
