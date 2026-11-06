/*
 * Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.ui.code_editor.scheme

import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

class SchemeIDEADark: EditorColorScheme(true) {
    override fun applyDefault() {
        super.applyDefault()
        setColor(WHOLE_BACKGROUND, 0xFF10090B.toInt())
        setColor(TEXT_NORMAL, 0xFFF1E8EA.toInt())
        setColor(CURRENT_LINE, 0xFF211116.toInt())
        setColor(SELECTED_TEXT_BACKGROUND, 0xFF5A2430.toInt())
        setColor(SELECTION_INSERT, 0xFFFFDDE1.toInt())
        setColor(LINE_NUMBER_BACKGROUND, 0xFF160B0E.toInt())
        setColor(LINE_NUMBER, 0xFF765A60.toInt())
        setColor(LINE_NUMBER_CURRENT, 0xFFE8B8BF.toInt())
        setColor(LINE_DIVIDER, 0xFF3A2026.toInt())
        setColor(BLOCK_LINE, 0xFF2E1A20.toInt())
        setColor(NON_PRINTABLE_CHAR, 0xFF8B6870.toInt())
        setColor(SCROLL_BAR_THUMB, 0xFF8C555F.toInt())
        setColor(SCROLL_BAR_THUMB_PRESSED, 0xFFD04A5A.toInt())
        setColor(COMPLETION_WND_BACKGROUND, 0xFF241217.toInt())

        //语法高亮
        setColor(KEYWORD, 0xFFFF8B75.toInt())
        setColor(COMMENT, 0xFF7A7E85.toInt())
        setColor(LITERAL, 0xFF6AAB73.toInt())
        setColor(OPERATOR, 0xFFBCBEC4.toInt())
        setColor(IDENTIFIER_NAME, 0xFFBCBEC4.toInt())
        setColor(IDENTIFIER_VAR, 0xFFFFA0B5.toInt())
        setColor(FUNCTION_NAME, 0xFF92B8F2.toInt())
        setColor(ANNOTATION, 0xFFB3AE60.toInt())
        setColor(HTML_TAG, 0xFFFF8B9A.toInt())
        setColor(ATTRIBUTE_NAME, 0xFFBCBEC4.toInt())
        setColor(ATTRIBUTE_VALUE, 0xFF6AAB73.toInt())

        //问题诊断
        setColor(PROBLEM_ERROR, 0xFFFA6675.toInt())
        setColor(PROBLEM_WARNING, 0xFFF2C55C.toInt())
        setColor(PROBLEM_TYPO, 0xFF7EC482.toInt())

        //其他
        setColor(MATCHED_TEXT_BACKGROUND, 0xFF51212C.toInt())
        setColor(HIGHLIGHTED_DELIMITERS_BACKGROUND, 0xFF49232B.toInt())
    }
}