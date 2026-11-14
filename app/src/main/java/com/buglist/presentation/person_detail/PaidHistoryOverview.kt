package com.buglist.presentation.person_detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.buglist.R
import com.buglist.presentation.theme.BugListColors

@Composable
internal fun PaidHistoryOverview(onCopy: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().background(BugListColors.Surface).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(onClick = onCopy) {
            Text(stringResource(R.string.paid_copy_list), color = BugListColors.Gold)
        }
    }
}
