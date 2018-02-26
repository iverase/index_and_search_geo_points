# index_and_search_geo_points

## Intro

This project allows to index a provided list of points, defined by latitude and longitude, into an in-memory BKD tree and query the input data using 
bounding boxes.

## Description of the in-memory tree

The tree is a complete binary tree holding a configurable maximum number of points per leaf nodes.
The number of points  to index and the maximum number of points per leaf defines the depth of the tree ensuring
a minimum occupancy of 50% on each of the leaf nodes and a naximum occupancy of the provided number.
The tree does not have any pointer between the nodes and relays on the numbering (node id) of the nodes for
navigating the structure. The memory footprint is low and holds the following data structures:

* A sorted array of documents: All documents belonging to a node are group together. The size of the array is the numer of indexed doxuments.
* A sorted array of bounding boxes: Gets the bounding box of a tree node by node id. The size of the array is the number of nodes in the tree.  
* A sorted array of document offsets for leaf nodes: It get the start and end index in the array of documents for each leaf node.  The size
of the array is the number of leaf nodes.

This implementation uses a bulk loading mechanism to build the tree that requires only three passes of the input points. One for sorting the points by longitude, one for 
sorting the points by latitude for each longitude partition and one to build the bounding boxes. After the tree has been built it 
cannot be modified and can only be used for performing spatial queries. 

The spatial queries are performed using the clasical approach: Starting from the root node, it will check the spatial relationship of the provided bounding box
with the bonding box of the current node. If the relationship is disjoint, then it ignores that art of the tree, if within then it will collect all documents
undder that node, else it will go one level down and perform the same operation excet for leaf nodes where it will then check the points one by one against
the provided bounding box.

## Compiling the project

First you need to clone this project into your local disk using git clone:

`git clone https://github.com/iverase/index_and_search_geo_points.git`

The easiest way to build the project is using gradle. Go to the root directory of the project and perform a gradle build:

`gradle build`     

A executable jar called index_and_search_geo_points.jar will be created under build/libs directory.  

## Running the program

The program expects two inuts, the first parameters is the path to the points file and the second is the path to the queries file:

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

In addition, a third optional parameter can be given which sets the documtens per leaf the tree must comply. If not provided
it will use the dault value of 1024. This value must be an integer and must be bigger than one:

`java -jar index_and_search_geo_points.jar /path/to/points.csv /path/to/queries 512`

After running this command, the program will load the points in memory, build the index and perform the queries.  
   
