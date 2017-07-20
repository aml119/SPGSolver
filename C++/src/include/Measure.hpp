#ifndef MEASURE_HPP
#define MEASURE_HPP

typedef std::map<int, std::vector<int>> PriorityMap;

class Measure
{
public:
    Measure(int length);
    Measure(Measure& other);
    bool isTop();
    std::vector<int> get_measure();
    static Measure least_above(Measure other, int p_trunc);
    static void init_max(PriorityMap priority_map, int highest_priority);

private:
    static std::vector<int> max;
    bool top;
    std::vector<int> measure;
};

#endif
