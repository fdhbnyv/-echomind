path = "D:\\EchoMind\\android\\app\\src\\main\\java\\com\\echomind\\app\\ui\\screens\\HomeScreen.kt"
with open(path, 'r', encoding='utf-8') as f:
    t = f.read()

old_part = """                )
            }
        }
    }
}

// ──────────────────────────────────────
// Recent entries list"""

new_part = """                )
            }

            // Calendar button: add action items with deadlines to system calendar
            if (note.actionItems.isNotEmpty()) {
                val ctx = androidx.compose.ui.platform.LocalContext.current
                Button(
                    onClick = {
                        val helper = com.echomind.app.service.CalendarHelper
                        for (item in note.actionItems) {
                            val deadlineMatch = Regex("(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})").find(item)
                            val ms = deadlineMatch?.let { helper.parseDeadlineToMillis(it.value) }
                            helper.insertEvent(ctx, item.take(50), startTimeMs = ms)
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Success),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(36.dp),
                ) {
                    Text("\U0001F4C5 添加到日历", fontSize = 13.sp)
                }
            }
        }
    }
}

// Recent entries list"""

if old_part in t:
    t = t.replace(old_part, new_part, 1)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(t)
    print(f"OK - replaced. New size: {len(t)} bytes")
else:
    print("Pattern NOT FOUND")
    # Debug: find the matching area
    import re
    # Find 'Recent entries list'
    idx = t.find('Recent entries list')
    if idx > 0:
        print(f"Found 'Recent entries list' at {idx}")
        print(repr(t[idx-200:idx+50]))
