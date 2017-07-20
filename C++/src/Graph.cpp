#include <vector>
#include <map>

#include "include/Graph.hpp"

Graph::Graph(int numNodes)
{
    nodes = std::vector<Node>(numNodes);
    highest_priority = -1;
}

Node& Graph::get(long n)
{
    return nodes[n];
}

void Graph::addNode(int node, int priority, int player)
{
    priorityMap[priority].push_back(node);
    nodes[node].set_priority(priority);
    nodes[node].set_player(player);
}

void Graph::addEdge(int origin, int destination)
{
    nodes[origin].add_adj(destination);
    nodes[destination].add_inj(origin);
}

long Graph::size()
{
    return nodes.size();
}

int Graph::get_highest_priority()
{
    if (highest_priority != -1) return highest_priority;

    for (auto it = priorityMap.begin(); it != priorityMap.end(); it++)
    {
        if (it->first > highest_priority) highest_priority = it->first;
    }
    return highest_priority;
}

PriorityMap& Graph::get_priority_map()
{
    return priorityMap;
}

std::vector<Node>& Graph::get_nodes()
{
    return nodes;
}
