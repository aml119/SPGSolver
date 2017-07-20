#include <vector>
#include <map>

#include "include/Solution.hpp"

// TODO implement this
void Solution::print_solution()
{
    
}

void Solution::add_even_winning_node(int node)
{
    even_winning_set.push_back(node);
}

void Solution::add_odd_winning_node(int node)
{
    odd_winning_set.push_back(node);
}

void Solution::add_even_strategy(int node_from, int node_to)
{
    even_strategy.push_back({ from, to });
}

void Solution::add_odd_strategy(int node_from, int node_to)
{
    odd_strategy.push_back({ from, to });
}
