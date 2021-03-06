import pygame
import time

name = "test.mp3"
def start_music():
    pygame.mixer.init()
    pygame.mixer.music.set_volume(1.0)

def playMusic(name):
    pygame.mixer.music.load(name)
    pygame.mixer.music.play()

# num is in milliseconds
def fadeout(num):
    pygame.mixer.music.fadeout(num)

def isPlaying():
    return pygame.mixer.music.get_busy()
