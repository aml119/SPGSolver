#ifndef SOLUTION_HPP
#define SOLUTION_HPP

class Solution
{
public:
    void print_solution();
    void add_even_winning_node(int node);
    void add_odd_winning_node(int node);
    void add_even_strategy(int node_from, int node_to);
    void add_odd_strategy(int node_from, int node_to);

private:
    std::vector<int> even_winning_set;
    std::vector<int> odd_winning_set;
    std::map<Node, Node> even_strategy;
    std::map<Node, Node> odd_strategy;
};

#endif
