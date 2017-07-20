#include <vector>

#include "include/Node.hpp"

Node::Node() {
    priority = -1;
    player = -1;
}
void Node::set_priority(int pr) {
    priority = pr;
}
void Node::set_player(int pl) {
    player = pl;
}
int const Node::get_priority() {
    return priority;
}
int const Node::get_player() {
    return player;
}
std::vector<int> const& Node::get_adj() {
    return adj;
}
std::vector<int> const& Node::get_inj() {
    return inj;
}
void Node::add_adj(int other) {
    mutex.lock();
    adj.push_back(other);
    mutex.unlock();
}
void Node::add_inj(int other) {
    mutex.lock();
    inj.push_back(other);
    mutex.unlock();
}
