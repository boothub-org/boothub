package org.boothub.context

class GameProjectContext extends StandardProjectContext.Generic {
    static enum Genre {
        ACTION, SIMULATION, STRATEGY, OTHER
    }

    Genre genre
    boolean multiplayer
    String platform
}
