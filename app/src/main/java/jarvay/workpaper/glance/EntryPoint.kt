package jarvay.workpaper.glance

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jarvay.workpaper.Workpaper

@EntryPoint
@InstallIn(SingletonComponent::class)
interface EntryPoint {
    fun workpaper(): Workpaper
}