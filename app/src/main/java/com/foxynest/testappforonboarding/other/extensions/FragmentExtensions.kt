package com.foxynest.testappforonboarding.other.extensions

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController

fun Fragment.safeNavigate(currentId: Int, direction: NavDirections) {
    if (findNavController().currentDestination?.id == currentId) {
        findNavController().navigate(direction)
    }
}

// Extension функция для safeNavigate с аргументами
fun Fragment.safeNavigate(currentId: Int, actionId: Int, args: Bundle? = null) {
    if (findNavController().currentDestination?.id == currentId) {
        if (args != null) {
            findNavController().navigate(actionId, args)
        } else {
            findNavController().navigate(actionId)
        }
    }
}