# Notification fields

We want to filter the notification at a very high grain, while making the UI simple to use.
There are 3 levels of filtering.

1. By package name

    This represents the app in its entirety.
    At this level, you can have one indicator representing any notification posted by the app.

2. By channel ID

    We can only retrieve the channel ID (not its name), but this is useful for many apps,
    and matches what's shown in the notification setting screen.
    
3. Other fields

    There are many text fields associated with notifications, and the semantics are
    mostly related to the presentation, not the content.
    However, we group these fields in 4 categories to make the filtering more useful.
    
    | Category | Notification extras                         | Gmail | WhatsApp | SMS |
    |----------|---------------------------------------------|-------|----------|-----|
    | Title    | `EXTRA_TITLE`<br>`EXTRA_CONVERSATION_TITLE` |       |          |     |
    | Info     | `EXTRA_INFO_TEXT`<br>`EXTRA_SUMMARY_TEXT`   |       |          |     |
    | Subtitle | `EXTRA_SUB_TEXT`<br>`EXTRA_PEOPLE_LIST`     |       |          |     |
    | Text     | `EXTRA_TEXT`<br>`EXTRA_BIG_TEXT`            |       |          |     |
