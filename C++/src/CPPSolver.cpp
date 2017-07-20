//
//  CPPSolver.cpp
//  C++ Parity Games Solver
//

#include <array>
#include <chrono>
#include <fstream>
#include <future>
#include <iostream>
#include <map>
#include <vector>
#include <boost/algorithm/string/classification.hpp>
#include <boost/algorithm/string/split.hpp>
#include <boost/program_options.hpp>
#include <boost/dynamic_bitset.hpp>

#include "include/Node.hpp"
#include "include/Graph.hpp"
#include "include/Measure.hpp"
#include "include/Solution.hpp"

typedef typename std::map<int, std::vector<int>> PriorityMap;
typedef typename std::map<Node, Measure> ProgressMeasure;

// returns the highest priority node in the graph that has not been removed
int max_priority(Graph& G, std::vector<bool>& removed)
{
    int max = -1;
    for (long v = 0; v < G.size(); v++)
    {
        if (!removed[v] && G.get(v).get_priority() > max)
        {
            max = G.get(v).get_priority();
        }
    }
    return max;
}

// called in parallel
std::vector<int>
async_attr(Graph* G, std::vector<int>* tmpMap, int inode, std::vector<bool>* removed, int i)
{
    std::vector<int> A;
    Node* node = &G->get(inode);
    for (const int v0 : node->get_inj()) {
        if (!(*removed)[v0]) {
            G->get(v0).mutex.lock();
            auto flag = G->get(v0).get_player() == i;
            if ((*tmpMap)[v0] == -1) {
                if (flag) {
                    A.push_back(v0);
                    (*tmpMap)[v0] = 0;
                } else {
                    int adj_counter = -1;
                    for (const int x : G->get(v0).get_adj()) {
                        if (!(*removed)[x]) {
                            adj_counter += 1;
                        }
                    }
                    (*tmpMap)[v0] += adj_counter;
                    if (adj_counter == 0) {
                        A.push_back(v0);
                    }
                }
            } else if (!flag and (*tmpMap)[v0] > 0) {
                (*tmpMap)[v0] -= 1;
                if ((*tmpMap)[v0] == 0) {
                    A.push_back(v0);
                }
            }
            G->get(v0).mutex.unlock();
        }
    }
    return A;
}

// calls async_attr on multiple threads
std::vector<int>
concurrent_attr(Graph& G, std::vector<bool>& removed, std::vector<int>& A, int i)
{
    std::vector<int> tmpMap(G.size(), -1);
    std::vector<bool> check(G.size());
    for (const int x : A)
    {
        tmpMap[x] = 0;
        check[x] = true;
    }

    int index = 0;
    std::vector<std::future<std::vector<int>>> results;
    while (index < A.size())
    {
        while (index < A.size())
        {
            results.push_back(std::async(async_attr, &G, &tmpMap, A[index], &removed, i));
            index += 1;
        }
        for (int i = 0; i < results.size(); i++)
        {
            auto res = results[i].get();
            for (auto i : res)
            {
                if (!check[i])
                {
                    check[i] = true;
                    A.push_back(i);
                }
            }
        }
        results.clear();
    }

    return A;
}

// the attractor used in the serial zielonka
std::vector<int>
attr(Graph& G, std::vector<bool>& removed, std::vector<int>& A, int i)
{
    std::vector<int> tmpMap(G.size(), -1);
    for (const int x : A)
    {
        tmpMap[x] = 0;
    }
    auto index = 0;
    while (index < A.size())
    {
        for (const int v0 : G.get(A[index]).get_inj())
        {
            if (!removed[v0])
            {
                auto flag = G.get(v0).get_player() == i;
                if (tmpMap[v0] == -1)
                {
                    if (flag)
                    {
                        A.push_back(v0);
                        tmpMap[v0] = 0;
                    }
                    else
                    {
                        int adj_counter = -1;
                        for (const int x : G.get(v0).get_adj())
                        {
                            if (!removed[x])
                            {
                                adj_counter += 1;
                            }
                        }
                        tmpMap[v0] = adj_counter;
                        if (adj_counter == 0)
                        {
                            A.push_back(v0);
                        }
                    }
                }
                else if (!flag and tmpMap[v0] > 0)
                {
                    tmpMap[v0] -= 1;
                    if (tmpMap[v0] == 0)
                    {
                        A.push_back(v0);
                    }
                }
            }
        }
        index += 1;
    }
    return A;
}

// parallel implementation of zielonka
std::array<std::vector<int>, 2>
win_concurrent(Graph& G, std::vector<bool>& removed)
{
    std::array<std::vector<int>, 2> W;
    auto d = max_priority(G, removed);
    if (d > -1) {
        std::vector<int> U;
        for (const int x : G.get_priority_map()[d])
        {
            if (!removed[x])
            {
                U.push_back(x);
            }
        }
        int p = d % 2;
        int j = 1 - p;
        std::array<std::vector<int>, 2> W1;
        auto A = concurrent_attr(G, removed, U, p);
        std::vector<bool> removed1(removed);
        for (const int x : A)
        {
            removed1[x] = true;
        }
        W1 = win_concurrent(G, removed1);
        if (W1[j].size() == 0)
        {
            std::merge(W1[p].begin(), W1[p].end(), A.begin(), A.end(),
                       std::back_inserter(W[p]));
        }
        else
        {
            auto B = concurrent_attr(G, removed, W1[j], j);
            std::vector<bool> removed2(removed);
            for (const int x : B)
            {
                removed2[x] = true;
            }
            W1 = win_concurrent(G, removed2);
            W[p] = W1[p];
            std::merge(W1[j].begin(), W1[j].end(), B.begin(), B.end(),
                       std::back_inserter(W[j]));
        }
    }
    return W;
}

// serial implementation of zielonka
std::array<std::vector<int>, 2>
win_improved(Graph& G, std::vector<bool>& removed)
{
    std::array<std::vector<int>, 2> W;
    auto d = max_priority(G, removed);
    if (d > -1) {
        std::vector<int> U;
        for (const int x : G.get_priority_map()[d])
        {
            if (!removed[x])
            {
                U.push_back(x);
            }
        }
        int p = d % 2;
        int j = 1 - p;
        std::array<std::vector<int>, 2> W1;
        auto A = attr(G, removed, U, p);
        std::vector<bool> removed1(removed);
        for (const int x : A)
        {
            removed1[x] = true;
        }
        W1 = win_improved(G, removed1);
        if (W1[j].size() == 0)
        {
            std::merge(W1[p].begin(), W1[p].end(), A.begin(), A.end(),
                       std::back_inserter(W[p]));
        }
        else
        {
            auto B = attr(G, removed, W1[j], j);
            std::vector<bool> removed2(removed);
            for (const int x : B)
            {
                removed2[x] = true;
            }
            W1 = win_improved(G, removed2);
            W[p] = W1[p];
            std::merge(W1[j].begin(), W1[j].end(), B.begin(), B.end(),
                       std::back_inserter(W[j]));
        }
    }
    return W;
}

// Solution small_progress_measures(Graph& G, std::vector<bool>& removed)
// {
//     // initialise progress measures for all nodes
//
//     // create a queue of nodes to lift
//     // attempt to lift each node on the queue
//     // if a lift is successful, add all predecessors of that node to the queue.
//     // once lifting is complete, populate solution object with...
//     // winning sets for each player
//     // winning strategies for each player
// }
//
// // returns a new measure for the node after lifting, or null if it is not lifted.
// Measure lift(ProgressMeasure progress_measures, Node node)
// {
//     // get all nodes reached from outgoing edges and get the progressed measure
//     // if node is even, choose the minimum progressed measure,
//     // if node is odd, choose the maximum progressed measure.
// }
//
// // returns mu(to) if from is even, or least measure > mu(to) if from is odd.
// Measure progressed_measure(ProgressMeasure progress_measures, Node from, Node to)
// {
//     Measure to_measure = progress_measures[to];
//     Measure from_measure = progress_measures[from];
//     if (to.get_player() == 0)   // then this is the even player
//     {
//         if (to_measure >= from_measure) return to_measure;
//         else return from_measure;
//     }
//     else
//     {
//         if (to_measure > from_measure) return to_measure;
//         else
//         {
//             return Measure.least_above(from_measure);
//         }
//     }
// }

// finds the winning sets for both players and returns them.
// pass the algorithm to solve the parity game to this function.
std::array<std::vector<int>, 2>
win(Graph& G, std::function<std::array<std::vector<int>, 2>(Graph& G, std::vector<bool>&)> f)
{
    std::chrono::time_point<std::chrono::system_clock> start, end;
    start = std::chrono::system_clock::now();
    auto removed = std::vector<bool>(G.size(), false);
    auto res = f(G, removed);
    end = std::chrono::system_clock::now();
    std::chrono::duration<double> elapsed_seconds = end-start;
    printf("Solved in ........... %fs \n", elapsed_seconds.count());
    return res;
}

// takes a single line of a parity game file, and converts it into a node.
void add_node_string(Graph *G, std::string line, std::mutex *gLock)
{
    std::vector<std::string> x, edges;
    boost::split(x, line, boost::is_any_of(" "));
    int node = std::atoi(x[0].c_str());
    gLock->lock();
    G->addNode(node, std::atoi(x[1].c_str()), std::atoi(x[2].c_str()));
    gLock->unlock();
    boost::split(edges, x[3], boost::is_any_of(","));
    for (const auto& x : edges)
    {
        G->addEdge(node, atoi(x.c_str()));
    }

}

Graph init_graph_from_file(std::string argf)
{
    std::chrono::time_point<std::chrono::system_clock> start, end;
    start = std::chrono::system_clock::now();
    std::ifstream ifs(argf);
    std::string line;
    std::string first;
    std::getline(ifs, first);
    int numNodes = 0;
    if (first.compare("parity") > -1)
    {
        std::vector<std::string> y;
        boost::split(y, first, boost::is_any_of(" "));
        numNodes = atoi(y[1].substr(0, y[1].size()-1).c_str());
    }
    else
    {
        throw "Invalid file Passed as argument.";
    }
    Graph G(numNodes + 1);
    std::mutex gLock;
    std::vector<std::future<void>> results;
    while (std::getline(ifs, line))
    {
        results.push_back(std::async(add_node_string, &G, std::string(line), &gLock));
    }
    ifs.close();
    for (int i = 0; i < results.size(); i++)
    {
        results[i].wait();
    }
    end = std::chrono::system_clock::now();
    std::chrono::duration<double> elapsed_seconds = end-start;
    std::cout << "Parsed in ........... " << elapsed_seconds.count() << "s" << std::endl;
    return G;
}

int main(int argc, const char * argv[])
{
    namespace po = boost::program_options;
    po::options_description desc("Options");
    desc.add_options()
    ("help,h", "Help Screen")
    ("concurrent", po::value<bool>()->default_value(false), "Use concurrent solver")
    ("files", po::value<std::vector<std::string>>(), "Input Files")
    ("justHeat", po::value<bool>()->default_value(false), "Prevent to print solutions");

    po::variables_map variables_map;{}
    po::positional_options_description files;
    files.add("files", 1);

    try
    {
        po::store(po::command_line_parser(argc, argv).options(desc).positional(files).run(), variables_map);
        po::notify(variables_map);
    }
    catch (po::error& e)
    {
        std::cerr << std::endl << e.what() << std::endl;
        return 1;
    }

    if (!variables_map.count("files"))
    {
        std::cerr << "No file specified. Aborting!" << std::endl;
        return 1;
    }
    else if (variables_map.count("files") > 1)
    {
        std::cerr << "WARN: Multiple files found, solving the first one only." << std::endl;
    }

    auto file_paths = variables_map["files"].as<std::vector<std::string>>();

    std::cout << "Parsing from ........ " << file_paths[0] << std::endl;
    auto G = init_graph_from_file(file_paths[0]);

    std::array<std::vector<int>, 2> solutions;

    if (variables_map["concurrent"].as<bool>())
    {
        solutions = win(G, win_concurrent);
    }
    else
    {
        solutions = win(G, win_improved);
    }



    if (variables_map["justHeat"].as<bool>())
    {
        return 0;
    }

    std::cout << "\nSolution for Player 0:" << std::endl;
    std::sort(solutions[0].begin(), solutions[0].end());
    std::sort(solutions[1].begin(), solutions[1].end());
    std::cout << "{";
    for (auto &v : solutions[0])
    {
        if (v == solutions[0][solutions[0].size()-1])
        {
            printf("%d", v);
        }
        else
        {
            printf("%d, ", v);
        }
    }
    printf("}\n");
    printf("\nSolution for Player 1:\n{");
    for (auto &v : solutions[1])
    {
        if (v == solutions[1][solutions[1].size()-1])
        {
            printf("%d", v);
        }
        else
        {
            printf("%d, ", v);
        }
    }
    printf("}\n");
    return 0;
}
