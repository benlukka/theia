import * as d3 from "d3";
import { useEffect, useRef, useState } from "react";
import type { ChartComponent as Chart} from "../generated/models/ChartComponent";
import type {ChartDataPoint} from "../generated";

interface ChartComponentProps {
    chart?: Chart;
    height?: number;
    title?: string;
    showLegend?: boolean;
}

export function ChartComponent({
                                   chart,
                                   height = 350,
                                   title,
                               }: ChartComponentProps) {
    const svgRef = useRef<SVGSVGElement | null>(null);
    const containerRef = useRef<HTMLDivElement | null>(null);
    const [dimensions, setDimensions] = useState({ width: 0, height });

    useEffect(() => {
        if (!containerRef.current) return;
        const resizeObserver = new ResizeObserver(entries => {
            if (!entries[0]) return;
            const { width } = entries[0].contentRect;
            setDimensions({ width, height });
        });
        resizeObserver.observe(containerRef.current);
        return () => resizeObserver.disconnect();
    }, [height]);

    useEffect(() => {
        if (
            !svgRef.current ||
            !containerRef.current ||
            !chart ||
            chart.chartType !== "bar" ||
            !chart.data ||
            !Array.isArray(chart.data.data)
        ) {
            return;
        }

        const data: ChartDataPoint[] = chart.data.data;

        const svg = d3.select(svgRef.current);
        svg.selectAll("*").remove();

        const margin = { top: 30, right: 30, bottom: 70, left: 60 };
        const width = dimensions.width - margin.left - margin.right;
        const graphHeight = dimensions.height - margin.top - margin.bottom;

        const g = svg
            .attr("width", dimensions.width)
            .attr("height", dimensions.height)
            .append("g")
            .attr("transform", `translate(${margin.left},${margin.top})`);

        const maxValue = d3.max(data, d => d.value) || 0;

        // X scale: use label
        const x = d3.scaleBand()
            .domain(data.map(d => d.label))
            .range([0, width])
            .padding(0.3);

        // Y scale
        const y = d3.scaleLinear()
            .domain([0, maxValue * 1.1])
            .range([graphHeight, 0]);

        // X axis
        g.append("g")
            .attr("transform", `translate(0,${graphHeight})`)
            .call(d3.axisBottom(x))
            .selectAll("text")
            .attr("transform", "translate(-10,0)rotate(-45)")
            .style("text-anchor", "end")
            .style("font-size", "12px")
            .style("fill", "#ffffff");

        // Y axis
        g.append("g")
            .call(d3.axisLeft(y).ticks(5).tickFormat(d => d.toString()))
            .selectAll("text")
            .style("font-size", "12px")
            .style("fill", "#ffffff");

        // Bars
        g.selectAll(".bar")
            .data(data)
            .enter()
            .append("rect")
            .attr("class", "bar")
            .attr("x", d => x(d.label) || 0)
            .attr("y", graphHeight)
            .attr("width", x.bandwidth())
            .attr("height", 0)
            .attr("fill", chart.data.borderColor || "#4682b4")
            .attr("stroke", chart.data.borderColor || "#ffffff")
            .attr("stroke-width", 1)
            .attr("rx", 2)
            .attr("ry", 2)
            .transition()
            .duration(800)
            .delay((d, i) => i * 100)
            .attr("y", d => y(Number(d.value)))
            .attr("height", d => graphHeight - y(Number(d.value)));

        // Data labels
        // Data labels
        g.selectAll(".label")
            .data(data)
            .enter()
            .append("text")
            .attr("class", "label")
            .attr("x", d => (x(d.label) || 0) + x.bandwidth() / 2)
            .attr("y", d => y(Number(d.value)) - 5)
            .attr("text-anchor", "middle")
            .style("font-size", "10px")
            .style("fill", "#ffffff")
            .style("opacity", 0)
            .text(d => `${d.value} ${d.unit}`) // <-- Add unit here
            .transition()
            .duration(800)
            .delay((d, i) => i * 100 + 300)
            .style("opacity", 1);

        // Title
        if (title) {
            svg.append("text")
                .attr("x", dimensions.width / 2)
                .attr("y", 20)
                .attr("text-anchor", "middle")
                .style("font-size", "16px")
                .style("font-weight", "bold")
                .style("fill", "#333")
                .text(title);
        }
    }, [chart, dimensions]);

    if (!chart) return <div className="text-gray-500 p-4">No chart data available</div>;
    if (chart.chartType !== "bar") return <div className="text-gray-500 p-4">Chart type not supported</div>;

    return (
        <div
            className="chart-container rounded-lg shadow-md bg-white"
            ref={containerRef}
            style={{
                padding: '16px',
                margin: '10px 0',
                border: '1px solid #e2e8f0',
                borderRadius: '8px',
                width: '100%',
                height: `${height}px`,
                boxSizing: 'border-box',
                boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)'
            }}
        >
            {chart.title}
            {title && (title)}
            <svg
                ref={svgRef}
                style={{
                    width: '100%',
                    height: '100%',
                    overflow: 'visible'
                }}
            ></svg>
        </div>
    );
}