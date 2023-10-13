//
//  TestRoomCollectionViewCell.swift
//  TestVideoLoader
//
//  Created by wushengtao on 2023/8/30.
//

import UIKit

class TestRoomCollectionViewCell: UICollectionViewCell {
    public lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.textColor = .blue
        label.numberOfLines = 2
        label.textAlignment = .right
        label.font = UIFont.boldSystemFont(ofSize: 16)
        return label
    }()
    
    public lazy var canvasView = UIView()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        _loadSubview()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func _loadSubview() {
        canvasView.backgroundColor = .yellow
        contentView.addSubview(canvasView)
        addSubview(titleLabel)
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        canvasView.frame = bounds
        titleLabel.frame = bounds
        titleLabel.sizeToFit()
        titleLabel.frame = CGRect(x: frame.width - titleLabel.frame.width - 10, y: 80, width: titleLabel.bounds.width, height: titleLabel.bounds.height)
    }
}
