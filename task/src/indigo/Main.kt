package indigo

enum class Rank(val string: String, val points: Int = 0) {
    ACE("A", 1),
    TWO("2"),
    THREE("3"),
    FOUR("4"),
    FIVE("5"),
    SIX("6"),
    SEVEN("7"),
    EIGHT("8"),
    NINE("9"),
    TEN("10", 1),
    JACK("J", 1),
    QUEEN("Q", 1),
    KING("K", 1);
}

enum class Suit(val char: Char) {
    DIAMONDS('♦'),
    HEARTS('♥'),
    SPADES('♠'),
    CLUBS('♣');
}

data class Card(val suit: Suit, val rank: Rank) {
    override fun toString(): String {
        return "${rank.string}${suit.char}"
    }
}

class Deck {
    private val cards = MutableList(52) {
        val suit = it / 13
        val rank = it % 13
        Card(Suit.values()[suit], Rank.values()[rank])
    }

    init {
        cards.shuffle()
    }

    fun pop() = if (cards.isNotEmpty()) cards.removeAt(0) else null

    fun size() = cards.size
}
val deck = Deck()

open class Cards(size: Int) {
    val cards = MutableList(size) {
        deck.pop()!!
    }

    fun isEmpty() = cards.isEmpty()

    fun size() = cards.size
}

class Table: Cards(4) {
    override fun toString(): String {
        return cards.joinToString(" ")
    }

    fun add(card: Card) = cards.add(card)

    fun last() = cards.last()

    fun clear(): List<Card> {
        val result = cards.toList()
        cards.clear()
        return result
    }

    fun print() {
        if (cards.isEmpty()) {
            println("No cards on the table")
        } else {
            println("${cards.size} cards on the table, and the top card is ${cards.last()}")
        }
    }
}
val table = Table()

class Hand: Cards(6) {
    override fun toString(): String {
        var result = ""
        cards.forEachIndexed { index, card -> result += "${index + 1})$card " }
        return result
    }

    fun joinToString() = cards.joinToString(" ")

    fun deal() {
        if (deck.size() >= 6) {
            repeat(6) {
                cards.add(deck.pop()!!)
            }
        }
    }

    fun play(index: Int) = cards.removeAt(index)
}

class WinPile {
    private val cards = mutableListOf<Card>()

    fun add(card: Card) = cards.add(card)

    fun addAll(cards: Collection<Card>) = this.cards.addAll(cards)

    fun getPoints() = cards.sumOf { it.rank.points }

    fun size() = cards.size
}

enum class Action(string: String) {
    CARD("\\d"),
    EXIT("exit");

    val regex = string.toRegex()

    companion object {
        fun valueOfOrNull(string: String): Action? {
            values().forEach {
                if (string.matches(it.regex)) {
                    return it
                }
            }
            return null
        }
    }
}

enum class YesNo {
    YES, NO;

    companion object {
        fun valueOfOrNull(string: String): YesNo? = try {
            valueOf(string.uppercase())
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}

enum class Player(private val string: String) {
    PLAYER("Player"),
    COMPUTER("Computer");

    val hand = Hand()
    val winPile = WinPile()

    fun getScore(initialPlayer: Player? = null): Int {
        var score = winPile.getPoints()

        if (initialPlayer != null) {
            val myPile = winPile.size()
            val otherPile = values()[1 - ordinal].winPile.size()

            if (myPile > otherPile) {
                score += 3
            } else if (myPile == otherPile && this == initialPlayer) {
                score += 3
            }
        }

        return score
    }

    override fun toString(): String {
        return string
    }
}

var lastWinner = Player.PLAYER

fun main() {
    println("Indigo Card Game")

    val playFirst: YesNo
    while (true) {
        println("Play first?")
        val answer = YesNo.valueOfOrNull(readLine()!!)
        if (answer != null) {
            playFirst = answer
            break
        }
    }
    val initialPlayer = Player.values()[playFirst.ordinal]
    lastWinner = initialPlayer
    var currentPlayer = Player.values()[playFirst.ordinal]

    println("Initial cards on the table: $table")
    println()

    turn@while (true) {
        table.print()

        if (currentPlayer.hand.isEmpty()) {
            lastWinner.winPile.addAll(table.clear())
            printScore(initialPlayer)
            break@turn
        }

        when (currentPlayer) {
            Player.PLAYER -> {
                println("Cards in hand: ${Player.PLAYER.hand}")

                val cardIndex: Int
                while (true) {
                    println("Choose a card to play (1-${Player.PLAYER.hand.size()}):")
                    val response = readLine()!!
                    when (Action.valueOfOrNull(response)) {
                        Action.CARD -> {
                            val index = response.toInt()
                            if (index in 1 .. Player.PLAYER.hand.size()) {
                                cardIndex = index
                                break
                            }
                        }
                        Action.EXIT -> {
                            break@turn
                        }
                    }
                }

                val card = Player.PLAYER.hand.play(cardIndex - 1)
                play(Player.PLAYER, card)
            }
            Player.COMPUTER -> { // Computer
                println(Player.COMPUTER.hand.joinToString())

                val candidates = if (table.isEmpty())
                    emptyList()
                else {
                    val topCard = table.last()
                    Player.COMPUTER.hand.cards.filter { it.suit == topCard.suit || it.rank == topCard.rank }
                }

                val card: Card
                if (Player.COMPUTER.hand.size() == 1) {
                    card = Player.COMPUTER.hand.cards.first()
                } else if (candidates.size == 1) {
                    card = candidates.first()
                } else if (table.isEmpty() || candidates.isEmpty()) {
                    val groupedBySuit = Player.COMPUTER.hand.cards.groupBy { it.suit }
                    if (groupedBySuit.any { it.value.size > 1 }) {
                        card = groupedBySuit.filter { it.value.size > 1 }
                            .values
                            .flatten()
                            .random()
                    } else {
                        val groupedByRank = Player.COMPUTER.hand.cards.groupBy { it.rank }
                        if (groupedByRank.any { it.value.size > 1 }) {
                            card = groupedByRank.filter { it.value.size > 1 }
                                .values
                                .flatten()
                                .random()
                        } else {
                            card = Player.COMPUTER.hand.cards.random()
                        }
                    }
                } else {
                    // two or more candidates
                    val groupedBySuit = candidates.groupBy { it.suit }
                    if (groupedBySuit.any { it.value.size > 1 }) {
                        card = groupedBySuit.filter { it.value.size > 1 }
                            .values
                            .flatten()
                            .random()
                    } else {
                        val groupedByRank = candidates.groupBy { it.rank }
                        if (groupedByRank.any { it.value.size > 1 }) {
                            card = groupedByRank.filter { it.value.size > 1 }
                                .values
                                .flatten()
                                .random()
                        } else {
                            card = candidates.random()
                        }
                    }
                }

                Player.COMPUTER.hand.cards.remove(card)
                println("Computer plays $card")
                play(Player.COMPUTER, card)
            }
        }
        currentPlayer = Player.values()[1 - currentPlayer.ordinal]
    }

    println("Game Over")
    return
}

fun play(player: Player, card: Card) {
    val topCard = if (table.isEmpty()) null else table.last()
    if (topCard != null && (card.rank == topCard.rank || card.suit == topCard.suit)) {
        println("$player wins cards")
        player.winPile.addAll(table.clear())
        player.winPile.add(card)
        lastWinner = player

        printScore()
    } else {
        table.add(card)
    }
    println()

    if (player.hand.isEmpty())
        player.hand.deal()
}

fun printScore(initialPlayer: Player? = null) {
    println("Score: ${Player.PLAYER} ${Player.PLAYER.getScore(initialPlayer)} - ${Player.COMPUTER} ${Player.COMPUTER.getScore(initialPlayer)}")
    println("Cards: ${Player.PLAYER} ${Player.PLAYER.winPile.size()} - ${Player.COMPUTER} ${Player.COMPUTER.winPile.size()}")
}