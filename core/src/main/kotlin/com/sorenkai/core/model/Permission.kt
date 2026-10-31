package com.sorenkai.core.model

/**
 * Granular administrative permissions controlling access to administrative actions and resources.
 */
enum class Permission {
    /** Permission to moderate user discussions and comment threads. */
    MODERATE_DISCUSSIONS,

    /** Permission to temporarily suspend user accounts. */
    SUSPEND_ACCOUNTS,

    /** Permission to make final determinations and decisions on account suspensions. */
    DECIDE_SUSPENSIONS,

    /** Permission to permanently ban user accounts. */
    BAN_ACCOUNTS,

    /** Permission to manage (appoint and remove) moderators. */
    MANAGE_MODERATORS,

    /** Permission to review user appeals against administrative actions. */
    REVIEW_APPEALS,

    /** Permission to access and view site analytics and reporting metrics. */
    ACCESS_SITE_ANALYTICS,

    /** Permission to add and appoint new administrators. */
    ADD_ADMINS,

    /** Permission to remove and revoke administrator privileges. */
    REMOVE_ADMINS,
}
