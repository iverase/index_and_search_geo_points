# index_and_search_geo_points

## Intro

This project allows to index a provided list of points, defined by latitude and longitude, into an in-memory BKD tree and
query the input data using bounding boxes. A BKD tree as defined in this [paper](https://users.cs.duke.edu/~pankaj/publications/papers/bkd-sstd.pdf)
consists on a set (a forest) of static KDB trees. In this implementation, we crete as many as complete KDB trees as possible with
so final occupancy is close to 100%.

 The KDBs tree are built using the loading bulk mechanism described  on the paper.

## Description of the KDB tree

The KDB tree is a complete binary tree with a configurable maximum number of points per leaf nodes (it must be bigger than 1).
The tree does not have any pointer between the nodes and relays on the properties of a complete binary tree for
navigating the hierarchy. It holds the following data structures:

* A sorted array of points (documents): All documents belonging to a leaf node are group together. The size of the array is the number of
indexed points.
* A sorted array of bounding boxes: It allows to get the bounding box of a tree node by node id. The size of the array is the number
 of nodes in the tree.

This implementation uses a bulk loading mechanism to build the tree that requires only three passes of the input points. One for sorting
the points by longitude, one for sorting the points by latitude for each longitude partition and one to build the bounding boxes. After the
tree has been built it cannot be modified and can only be used for performing spatial queries. The division of the space is done evenly using
the longitude as the pivoting dimension. For example for level 1 we have just one block, a matrix 1X1 where first dimension is the longitude
and second dimension is the latitude. For level 2 a matrix 2X1, level 3 a matrix 2X2, level 4 a matrix 4X2, level 5 a matrix 4X4 and so on.

The spatial queries are performed using the classical approach. Starting from the root node, it will check the spatial relationship of the
provided bounding box with the bonding box of the current node. If the relationship is disjoint, then it ignores that part of the tree, if within
then it will collect all documents under that node, else it will go one level down and perform the same operation except for
leaf nodes where it will check the points under the node one by one against the provided bounding box.

## Compiling the project

First you need to clone this project into your local disk using git clone:

`git clone https://github.com/iverase/index_and_search_geo_points.git`

The project is built using [gradle](https://gradle.org/). From the root directory of the project execute the following command:

`gradle build`

The executable jar called index_and_search_geo_points.jar will be created under build/libs directory.

## Running the program

The program expects two inputs, the first parameters is the path to the points file and the second is the path to the
queries file:

`java -jar index_and_search_geo_points.jar /path/to/points.csv /path/to/queries`

The points file contains one geo point per line: a string id, then latitude,
then longitude, separated by one or more spaces. Here is an example:

```
London    51.509865   -0.118092
Paris     48.864716    2.349014
Berlin    52.520008   13.404954
New-York  40.730610  -73.935242
```

The queries file contains one bounding box query per line: minimum latitude, maximum
latitude, minimum longitude and maximum longitude, all separated by one or more spaces.
Here is an example:

```
45.349961 59.845233 -1.108073 13.972017
35.129304 60.117498 -90.452388 0.043210
```

In addition, a third optional parameter can be given which sets the number of points per leaf the tree must comply.
If not provided, it will use the default value of 1024 points per leaf. This value must be an integer and must be bigger than one:

`java -jar index_and_search_geo_points.jar /path/to/points.csv /path/to/queries 512`

If the input list is big, you might want to resize your heap memory using the following syntax, that in this case provides 6 gigabytes of heap space to the JVM.:

`java -Xmx6g -Xms6g -jar index_and_search_geo_points.jar /path/to/points.csv /path/to/queries 512`

After running the command, the program will load the points in memory, build the index, perform the queries and show the results.
