import kotlin.text.StringBuilder

fun main() {
    val input = readResourceLines("Day11Test2.txt")
    val output = findMinimumNumberOfMoves(input)
    println("The minimum number of steps is: $output")
}


private fun parseFloorPlan(input: List<String>): State {
    val materials = mutableListOf<String>()

    val floors = input.mapIndexed { index, line ->
        if(line.contains("nothing")) return@mapIndexed emptyList<Item>() to index

        val (_, itemPart) = line.split("contains a ")
        val itemStrings = itemPart.split(", a ").flatMap { it.split(" and a ") }
        val items = itemStrings.map { generatorString ->
            val material = generatorString.split(" ", "-").first()
            val isGenerator = generatorString.contains("generator")

            if(!materials.contains(material)) materials.add(material)
            Item(materials.indexOf(material), isGenerator)
        }

        items to index
    }

    val sortedFloors = floors.sortedBy { it.second }.map { it.first }
    return State(sortedFloors, 0)
}

private fun findMinimumNumberOfMoves(input: List<String>): Int {
    val state = parseFloorPlan(input)

    return breadthFirstSearch(listOf(state), 0)
}

val reachedPosition: MutableMap<State, Int> = mutableMapOf()
fun breadthFirstSearch(statesToExplore: List<State>, stepCount: Int): Int {
    val futureStates = statesToExplore.mapNotNull { currentState ->
        //caching
        when (val steps = reachedPosition[currentState]) {
            is Int -> {
                if (steps <= stepCount) return@mapNotNull null
            }

            null -> {
                reachedPosition[currentState] = stepCount
            }
        }

        val currentFloorIndex = currentState.currentFloorIndex
        //val currentFloor = currentState.floors[currentFloorIndex]
        val newTargetFloors = listOf(-1, 1).mapNotNull {
            val newFloor = it + currentFloorIndex
            if (newFloor in 0..3) newFloor else null
        }

        val currentFloor = currentState.currentFloor
        val itemsToMove = currentFloor.flatMap { outer -> currentFloor.map { inner -> setOf(outer, inner) } }
        //find all possible combinations to fit in the elevator
        val filteredItems = itemsToMove.distinct().filter {
            if (it.size == 1) return@filter true
            val (first, second) = it.toList()
            if (first.isGenerator == second.isGenerator) return@filter true
            //we have a generator and a microchip at this point
            first.materialIndex == second.materialIndex
        }

        //filter out combinations where the current floor irradiates each other
        val validInElevatorItemsAndFloor = filteredItems.mapNotNull {
            //println(currentFloor)
            val floorWithRemovedItems = currentFloor - it
            val isLegal = floorWithRemovedItems.isLegalFloor()
            if (isLegal) it to floorWithRemovedItems else null
        }

        //filter out combinations where the target floor is invalid
        val validStates = validInElevatorItemsAndFloor.flatMap { (elevatorItems, newOriginFloor) ->
            newTargetFloors.mapNotNull { targetFloorIndex ->
                val currentTargetFloor = currentState.floors[targetFloorIndex]
                val newTargetFloor = currentTargetFloor + elevatorItems
                val isValid = newTargetFloor.isLegalFloor()
                if (!isValid) return@mapNotNull null

                //create new state overriding changing the floor where we came from and target floor
                val floors = currentState.floors.toMutableList()
                floors[targetFloorIndex] = newTargetFloor
                floors[currentState.currentFloorIndex] = newOriginFloor

                //val newState = currentState.copy(currentFloorIndex = targetFloorIndex)
                //newState.floors[newState.currentFloorIndex] = newTargetFloor.toMutableList()
                //newState.floors[currentState.currentFloorIndex] = newOriginFloor.toMutableList()

                State(floors.toList(), targetFloorIndex)
            }
        }

        val solvedState = validStates.find { it.isSolved() }
        if (solvedState != null) {
            println("solved")
            println(solvedState)
            return stepCount + 1
        }
        validStates
    }.flatten()

    return breadthFirstSearch(futureStates, stepCount + 1)
}

data class Item(val materialIndex: Int, val isGenerator: Boolean)

data class State(
    val floors: List<List<Item>>,
    val currentFloorIndex: Int = 0,
) {
    val currentFloor get() = floors[currentFloorIndex]
    private val itemCount = floors.sumOf { it.size }

    fun isSolved() = floors[3].size == itemCount

    override fun toString(): String {
        val itemCount = floors.flatMap { it }.count()

        val strings = floors.reversed().mapIndexed { index, floor ->
            val string = StringBuilder(itemCount + 2)
            string.append("F${4 - index} ")
            floor.forEach { string.append("${it.materialIndex}${if (it.isGenerator) "G" else "M"} ") }
            string.toString()
        }

        return strings.joinToString("\n")
    }
}

fun List<Item>.isLegalFloor(): Boolean {
    if(!this.containsGenerators()) return true
    if(!this.containsMicrochips()) return true

    val microchips = this.filter { !it.isGenerator }
    microchips.map {
        val generator = it.copy(isGenerator = true)
        if(!this.contains(generator)) return false
    }

    return true
}

private fun List<Item>.containsGenerators() = this.firstOrNull { it.isGenerator } != null
private fun List<Item>.containsMicrochips() = this.firstOrNull { !it.isGenerator } != null
