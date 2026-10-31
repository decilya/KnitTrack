package com.knittrac.app.data.local.exceptions

/**
 * Проект не найден при атомарном сохранении сессии.
 *
 * Бросает [com.knittrac.app.data.local.SessionDao.insertSessionAndIncrementProjectTotals],
 * когда UPDATE totals затронул 0 строк.
 */
class ProjectNotFoundException(projectId: Long) :
    Exception("Проект id=$projectId не найден")
