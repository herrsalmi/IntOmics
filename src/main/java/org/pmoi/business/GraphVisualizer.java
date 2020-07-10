package org.pmoi.business;

import org.pmoi.model.vis.VisGraph;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class GraphVisualizer {
    private static final String head = """
            <head>
                <title>Network</title>
                <script type="text/javascript" src="https://unpkg.com/vis-network/standalone/umd/vis-network.min.js"></script>
                <style type="text/css">
                        #mynetwork {
                            width: 100%;
                            height: 100vh;
                        }
                        #loadingBar {
                            position: absolute;
                            top: 0px;
                            left: 0px;
                            width: 100%;
                            height: 100%;
                            background-color: rgba(200, 200, 200, 0.8);
                            -webkit-transition: all 0.5s ease;
                            -moz-transition: all 0.5s ease;
                            -ms-transition: all 0.5s ease;
                            -o-transition: all 0.5s ease;
                            transition: all 0.5s ease;
                            opacity: 1;
                        }
                        #wrapper {
                            position: relative;
                            width: 100%;
                            height: 100%;
                        }
                        #text {
                            position: absolute;
                            top: 8px;
                            left: 530px;
                            width: 30px;
                            height: 50px;
                            margin: auto auto auto auto;
                            font-size: 22px;
                            color: #000000;
                        }
                        div.outerBorder {
                            position: relative;
                            top: 400px;
                            width: 600px;
                            height: 44px;
                            margin: auto auto auto auto;
                            border: 8px solid rgba(0, 0, 0, 0.1);
                            background: rgb(252, 252, 252); /* Old browsers */
                            background: -moz-linear-gradient(
                                    top,
                                    rgba(252, 252, 252, 1) 0%,
                                    rgba(237, 237, 237, 1) 100%
                            ); /* FF3.6+ */
                            background: -webkit-gradient(
                                    linear,
                                    left top,
                                    left bottom,
                                    color-stop(0%, rgba(252, 252, 252, 1)),
                                    color-stop(100%, rgba(237, 237, 237, 1))
                            ); /* Chrome,Safari4+ */
                            background: -webkit-linear-gradient(
                                    top,
                                    rgba(252, 252, 252, 1) 0%,
                                    rgba(237, 237, 237, 1) 100%
                            ); /* Chrome10+,Safari5.1+ */
                            background: -o-linear-gradient(
                                    top,
                                    rgba(252, 252, 252, 1) 0%,
                                    rgba(237, 237, 237, 1) 100%
                            ); /* Opera 11.10+ */
                            background: -ms-linear-gradient(
                                    top,
                                    rgba(252, 252, 252, 1) 0%,
                                    rgba(237, 237, 237, 1) 100%
                            ); /* IE10+ */
                            background: linear-gradient(
                                    to bottom,
                                    rgba(252, 252, 252, 1) 0%,
                                    rgba(237, 237, 237, 1) 100%
                            ); /* W3C */
                            filter: progid:DXImageTransform.Microsoft.gradient( startColorstr='#fcfcfc', endColorstr='#ededed',GradientType=0 ); /* IE6-9 */
                            border-radius: 72px;
                            box-shadow: 0px 0px 10px rgba(0, 0, 0, 0.2);
                        }
                        #border {
                            position: absolute;
                            top: 10px;
                            left: 10px;
                            width: 500px;
                            height: 23px;
                            margin: auto auto auto auto;
                            box-shadow: 0px 0px 4px rgba(0, 0, 0, 0.2);
                            border-radius: 10px;
                        }
                        #bar {
                            position: absolute;
                            top: 0px;
                            left: 0px;
                            width: 20px;
                            height: 20px;
                            margin: auto auto auto auto;
                            border-radius: 11px;
                            border: 2px solid rgba(30, 30, 30, 0.05);
                            background: rgb(0, 173, 246); /* Old browsers */
                            box-shadow: 2px 0px 4px rgba(0, 0, 0, 0.4);
                        }
                </style>
            </head>
            """;

    private static final String body = """
            <body>
                <div id="mynetwork"></div>
                <div id="loadingBar">
                    <div class="outerBorder">
                        <div id="text">0%</div>
                        <div id="border">
                            <div id="bar"></div>
                        </div>
                    </div>
                </div>
            </body>
            """;

    private static final String script = """
            <script type="text/javascript">
                var container = document.getElementById('mynetwork');
                var nodes = new vis.DataSet();
                var edges = new vis.DataSet();
                var data = {
                    nodes: nodes,
                    edges: edges
                };
                var options = {};
                var allNodes;
                var highlightActive = false;
                var network = new vis.Network(container, data, options);
                function setTheData(nodesArray,edgesArray) {
                    nodes = new vis.DataSet(nodesArray);
                    edges = new vis.DataSet(edgesArray);
                    network.setData({nodes:nodes, edges:edges});
                    allNodes = nodes.get({ returnType: "Object" });
                    network.on("click", neighbourhoodHighlight);
                    var options = {
                        nodes: {
                            shape: "dot",
                            size: 16
                        },
                        layout: {
                            randomSeed: 34
                        },
                        physics: {
                            forceAtlas2Based: {
                                gravitationalConstant: -26,
                                centralGravity: 0.005,
                                springLength: 230,
                                springConstant: 0.18
                            },
                            maxVelocity: 146,
                            solver: "forceAtlas2Based",
                            timestep: 0.35,
                            stabilization: {
                                enabled: true,
                                iterations: 2000,
                                updateInterval: 25
                            }
                        }
                    };
                    // {layout:{hierarchical:{sortMethod:'directed'}}}
                    network.setOptions(options);
                    network.on("stabilizationProgress", function(params) {
                        var maxWidth = 496;
                        var minWidth = 20;
                        var widthFactor = params.iterations / params.total;
                        var width = Math.max(minWidth, maxWidth * widthFactor);
                        document.getElementById("bar").style.width = width + "px";
                        document.getElementById("text").innerHTML =
                            Math.round(widthFactor * 100) + "%";
                    });
                    network.once("stabilizationIterationsDone", function() {
                        document.getElementById("text").innerHTML = "100%";
                        document.getElementById("bar").style.width = "496px";
                        document.getElementById("loadingBar").style.opacity = 0;
                        // really clean the dom element
                        setTimeout(function() {
                            document.getElementById("loadingBar").style.display = "none";
                        }, 500);
                    });
                }
                function neighbourhoodHighlight(params) {
                    // if something is selected:
                    if (params.nodes.length > 0) {
                        highlightActive = true;
                        var i, j;
                        var selectedNode = params.nodes[0];
                        var degrees = 2;
                        // mark all nodes as hard to read.
                        for (var nodeId in allNodes) {
                            allNodes[nodeId].color = "rgba(200,200,200,0.5)";
                            if (allNodes[nodeId].hiddenLabel === undefined) {
                                allNodes[nodeId].hiddenLabel = allNodes[nodeId].label;
                                allNodes[nodeId].label = undefined;
                            }
                        }
                        var connectedNodes = network.getConnectedNodes(selectedNode);
                        var allConnectedNodes = [];
                        // get the second degree nodes
                        for (i = 1; i < degrees; i++) {
                            for (j = 0; j < connectedNodes.length; j++) {
                                allConnectedNodes = allConnectedNodes.concat(
                                    network.getConnectedNodes(connectedNodes[j])
                                    );
                            }
                        }
                        // all second degree nodes get a different color and their label back
                        for (i = 0; i < allConnectedNodes.length; i++) {
                            allNodes[allConnectedNodes[i]].color = "rgba(150,150,150,0.75)";
                            if (allNodes[allConnectedNodes[i]].hiddenLabel !== undefined) {
                                allNodes[allConnectedNodes[i]].label =
                                    allNodes[allConnectedNodes[i]].hiddenLabel;
                                allNodes[allConnectedNodes[i]].hiddenLabel = undefined;
                            }
                        }
                        // all first degree nodes get their own color and their label back
                        for (i = 0; i < connectedNodes.length; i++) {
                            allNodes[connectedNodes[i]].color = undefined;
                            if (allNodes[connectedNodes[i]].hiddenLabel !== undefined) {
                                allNodes[connectedNodes[i]].label =
                                    allNodes[connectedNodes[i]].hiddenLabel;
                                allNodes[connectedNodes[i]].hiddenLabel = undefined;
                            }
                        }
                        // the main node gets its own color and its label back.
                        allNodes[selectedNode].color = undefined;
                        if (allNodes[selectedNode].hiddenLabel !== undefined) {
                            allNodes[selectedNode].label = allNodes[selectedNode].hiddenLabel;
                            allNodes[selectedNode].hiddenLabel = undefined;
                        }
                    } else if (highlightActive === true) {
                        // reset all nodes
                        for (var nodeId in allNodes) {
                            if (allNodes[nodeId].group == 1) {
                                allNodes[nodeId].color = "#3AC290";
                            } else {
                                allNodes[nodeId].color = "#44AAC2";
                            }
                            if (allNodes[nodeId].hiddenLabel !== undefined) {
                                allNodes[nodeId].label = allNodes[nodeId].hiddenLabel;
                                allNodes[nodeId].hiddenLabel = undefined;
                            }
                        }
                        highlightActive = false;
                    }
                    // transform the object into an array
                    var updateArray = [];
                    for (nodeId in allNodes) {
                        if (allNodes.hasOwnProperty(nodeId)) {
                            updateArray.push(allNodes[nodeId]);
                        }
                    }
                    nodes.update(updateArray);
                }
                window.addEventListener("load", () => {
                    init();
                });
            </script>
            """;

    public static void makeHTML(VisGraph graph) {
        String data = "setTheData(" + graph.getNodesJson() +  "," + graph.getEdgesJson() + ")";
        StringBuilder sb = new StringBuilder("<!doctype html>\n<html>\n");
        sb.append(head).append(body).append(script);
        sb.append("<script type=\"text/javascript\">").append('\n');
        sb.append("function init(){\n");
        sb.append(data).append('\n').append("}\n").append("</script>").append('\n');
        sb.append("</body>\n</html>");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("main.html"))){
            bw.write(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
