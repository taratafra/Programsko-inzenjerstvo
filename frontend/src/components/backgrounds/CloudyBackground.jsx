import "./CloudyBackground.css"
export default function CloudBackground({children}) {
    return (
        <div className="cloud-bg-root">
            <div className="clouds">
                <div id="o1"></div>
                <div id="o2"></div>
                <div id="o3"></div>
            </div>

            <div className="cloud-bg-content">
                {children}
            </div>
        </div>
    );
}

