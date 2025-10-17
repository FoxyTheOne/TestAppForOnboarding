package com.foxynest.testappforonboarding.other.extensions

import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController

fun Fragment.safeNavigate(currentId: Int, direction: NavDirections) {
    if (findNavController().currentDestination?.id == currentId) {
        findNavController().navigate(direction)
    }
}

fun Fragment.safeNavigate(currentId: Int, actionId: Int) {
    if (findNavController().currentDestination?.id == currentId) {
        findNavController().navigate(actionId)
    }
}