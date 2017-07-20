#ifndef NODE_HPP
#define NODE_HPP

#include <mutex>

class Node {
public:
    // variables
    std::mutex mutex;

    //functions
    Node();
    void set_priority(int pr);
    void set_player(int pl);
    int const get_priority();
    int const get_player();
    std::vector<int> const &get_adj();
    std::vector<int> const &get_inj();
    void add_adj(int other);
    void add_inj(int other);

private:
    // all nodes that this node has outgoing edges to
    std::vector<int> adj;
    // all nodes that have outgoing edges to this node
    std::vector<int> inj;
    int priority;
    int player;
};

#endif
