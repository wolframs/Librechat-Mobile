package com.garfiec.librechat.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

/**
 * Prevents an icon-only Compose action from silently disappearing from accessibility services.
 *
 * A null description is correct for decorative icons beside visible text, but never for the Icon
 * that is the only content of an IconButton. This deliberately narrow PSI rule avoids turning every
 * decorative image into duplicate TalkBack noise.
 */
class IconButtonContentDescriptionRule(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "IconButtonContentDescription",
        severity = Severity.Defect,
        description = "IconButton actions must expose a non-null contentDescription.",
        debt = Debt.FIVE_MINS,
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        if (expression.calleeName() != "IconButton") return

        expression.lambdaArguments
            .flatMap { it.getLambdaExpression()?.bodyExpression?.collectDescendantsOfType<KtCallExpression>().orEmpty() }
            .filter { it.calleeName() == "Icon" }
            .filter { icon ->
                icon.valueArguments.any { argument ->
                    argument.getArgumentName()?.asName?.asString() == "contentDescription" &&
                        argument.getArgumentExpression().isNullLiteral()
                }
            }
            .forEach { icon ->
                report(
                    CodeSmell(
                        issue,
                        Entity.from(icon),
                        "Icon inside IconButton has a null contentDescription. Use a localized action label.",
                    ),
                )
            }
    }

    private fun KtCallExpression.calleeName(): String? =
        (calleeExpression as? KtNameReferenceExpression)?.getReferencedName()

    private fun Any?.isNullLiteral(): Boolean =
        this is KtConstantExpression && text == "null"
}
