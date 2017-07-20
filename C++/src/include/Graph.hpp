#ifndef GRAPH_HPP
#define GRAPH_HPP

#include "Node.hpp"

// #include "Node.hpp"
typedef std::map<int, std::vector<int>> PriorityMap;

class Graph
{
public:
    Graph(int numNodes);
    Node& get(long n);
    void addNode(int node, int priority, int player);
    void addEdge(int origin, int destination);
    long size();
    int get_highest_priority();
    PriorityMap& get_priority_map();
    std::vector<Node>& get_nodes();

private:
    std::vector<Node> nodes;
    // maps all priorities to a list of nodes (indexes of nodes) with that priority
    std::map<int, std::vector<int>> priorityMap;
    int highest_priority;
};

#endif
