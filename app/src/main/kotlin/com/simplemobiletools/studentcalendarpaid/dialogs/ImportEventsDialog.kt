package com.simplemobiletools.studentcalendarpaid.dialogs

import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.studentcalendarpaid.extensions.config
import com.simplemobiletools.studentcalendarpaid.extensions.dbHelper
import com.simplemobiletools.studentcalendarpaid.helpers.DBHelper
import com.simplemobiletools.studentcalendarpaid.helpers.IcsImporter
import com.simplemobiletools.studentcalendarpaid.helpers.IcsImporter.ImportResult.*
import com.simplemobiletools.commons.extensions.setFillWithStroke
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.toast
import kotlinx.android.synthetic.main.dialog_import_events.view.*

class ImportEventsDialog(val activity: com.simplemobiletools.studentcalendarpaid.activities.SimpleActivity, val path: String, val callback: (refreshView: Boolean) -> Unit) {
    var currEventTypeId = DBHelper.REGULAR_EVENT_TYPE_ID
    var currEventTypeCalDAVCalendarId = 0

    init {
        val config = activity.config
        if (activity.dbHelper.getEventType(config.lastUsedLocalEventTypeId) == null) {
            config.lastUsedLocalEventTypeId = DBHelper.REGULAR_EVENT_TYPE_ID
        }

        val isLastCaldavCalendarOK = config.caldavSync && config.getSyncedCalendarIdsAsList().contains(config.lastUsedCaldavCalendarId.toString())
        currEventTypeId = if (isLastCaldavCalendarOK) {
            val lastUsedCalDAVCalendar = activity.dbHelper.getEventTypeWithCalDAVCalendarId(config.lastUsedCaldavCalendarId)
            if (lastUsedCalDAVCalendar != null) {
                currEventTypeCalDAVCalendarId = config.lastUsedCaldavCalendarId
                lastUsedCalDAVCalendar.id
            } else {
                DBHelper.REGULAR_EVENT_TYPE_ID
            }
        } else {
            config.lastUsedLocalEventTypeId
        }

        val view = (activity.layoutInflater.inflate(com.simplemobiletools.studentcalendarpaid.R.layout.dialog_import_events, null) as ViewGroup).apply {
            updateEventType(this)
            import_event_type_holder.setOnClickListener {
                SelectEventTypeDialog(activity, currEventTypeId, true) {
                    currEventTypeId = it.id
                    currEventTypeCalDAVCalendarId = it.caldavCalendarId

                    config.lastUsedLocalEventTypeId = it.id
                    config.lastUsedCaldavCalendarId = it.caldavCalendarId

                    updateEventType(this)
                }
            }
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(com.simplemobiletools.studentcalendarpaid.R.string.ok, null)
                .setNegativeButton(com.simplemobiletools.studentcalendarpaid.R.string.cancel, null)
                .create().apply {
                    activity.setupDialogStuff(view, this, com.simplemobiletools.studentcalendarpaid.R.string.import_events) {
                        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            activity.toast(com.simplemobiletools.studentcalendarpaid.R.string.importing)
                            Thread {
                                val overrideFileEventTypes = view.import_events_checkbox.isChecked
                                val result = IcsImporter(activity).importEvents(path, currEventTypeId, currEventTypeCalDAVCalendarId, overrideFileEventTypes)
                                handleParseResult(result)
                                dismiss()
                            }.start()
                        }
                    }
                }
    }

    private fun updateEventType(view: ViewGroup) {
        val eventType = activity.dbHelper.getEventType(currEventTypeId)
        view.import_event_type_title.text = eventType!!.getDisplayTitle()
        view.import_event_type_color.setFillWithStroke(eventType.color, activity.config.backgroundColor)
    }

    private fun handleParseResult(result: IcsImporter.ImportResult) {
        activity.toast(when (result) {
            IMPORT_OK -> com.simplemobiletools.studentcalendarpaid.R.string.importing_successful
            IMPORT_PARTIAL -> com.simplemobiletools.studentcalendarpaid.R.string.importing_some_entries_failed
            else -> com.simplemobiletools.studentcalendarpaid.R.string.importing_failed
        })
        callback(result != IMPORT_FAIL)
    }
}
