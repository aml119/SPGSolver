#include <vector>
#include <map>

#include "include/Measure.hpp"

Measure::Measure(int length)
{
    top = false;
}

Measure::Measure(Measure other)
{
    top = other.isTop;
    Measure new_measure(other.get_measure())
    measure = new_measure;
}

bool Measure::isTop()
{
    return top;
}

std::vector<int> Measure::get_measure()
{
    return measure;
}

// TODO implement this
static Measure Measure::least_above(Measure other, int p_trunc)
{
    Measure new_measure;
    if (other.get_measure()[p_trunc] < max[p_trunc])
    {

    }
    return new_measure;
}

static void Measure::init_max(PriorityMap priority_map, int highest_priority)
{
    // traverse priorities
    for (int i = 0; i <= highest_priority; i++)
    {
        max[i] = (i % 2 == 0) : 0 ? priority_map[i].size();
    }
}
