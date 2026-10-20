package ru.pacani.fitnessdiary.ui.auth

import androidx.annotation.StringRes
import ru.pacani.fitnessdiary.R
import ru.pacani.fitnessdiary.domain.model.Goal
import ru.pacani.fitnessdiary.domain.validation.ValidationError

/** Текст ошибки поля для пользователя. */
@StringRes
fun ValidationError.messageRes(): Int = when (this) {
    ValidationError.REQUIRED -> R.string.error_required
    ValidationError.EMAIL_INVALID -> R.string.error_email_invalid
    ValidationError.PASSWORD_TOO_SHORT -> R.string.error_password_too_short
    ValidationError.PASSWORD_NEEDS_LETTER_AND_DIGIT -> R.string.error_password_letter_digit
    ValidationError.PASSWORDS_DO_NOT_MATCH -> R.string.error_passwords_do_not_match
    ValidationError.NAME_LENGTH -> R.string.error_name_length
    ValidationError.NOT_A_NUMBER -> R.string.error_not_a_number
    ValidationError.HEIGHT_OUT_OF_RANGE -> R.string.error_height_range
    ValidationError.WEIGHT_OUT_OF_RANGE -> R.string.error_weight_range
}

/** Название цели для чипов Регистрации и Профиля. */
@StringRes
fun Goal.labelRes(): Int = when (this) {
    Goal.STRENGTH -> R.string.goal_strength
    Goal.MASS -> R.string.goal_mass
    Goal.WEIGHT_LOSS -> R.string.goal_weight_loss
    Goal.MAINTENANCE -> R.string.goal_maintenance
}
